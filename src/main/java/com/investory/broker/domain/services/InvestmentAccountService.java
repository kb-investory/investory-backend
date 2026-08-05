package com.investory.broker.domain.services;

import com.investory.broker.domain.exception.BrokerErrorCode;
import com.investory.broker.domain.exception.BrokerException;
import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.ports.HoldingDetailPort;
import com.investory.broker.domain.ports.HoldingSummaryPort;
import com.investory.broker.domain.ports.dto.AccountHoldingsInfo;
import com.investory.broker.domain.ports.dto.HoldingDetailInfo;
import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.domain.services.dto.query.GetAccountDetailQuery;
import com.investory.broker.domain.services.dto.query.GetConnectionAccountsQuery;
import com.investory.broker.domain.services.dto.result.AccountDetailResult;
import com.investory.broker.domain.services.dto.result.AccountListResult;
import com.investory.broker.domain.services.dto.result.ConnectionAccountsResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvestmentAccountService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final BrokerConnectionRepository brokerConnectionRepository;
    private final HoldingSummaryPort holdingSummaryPort;
    private final HoldingDetailPort holdingDetailPort;

    public InvestmentAccountService(
            InvestmentAccountRepository investmentAccountRepository,
            BrokerConnectionRepository brokerConnectionRepository,
            HoldingSummaryPort holdingSummaryPort,
            HoldingDetailPort holdingDetailPort) {
        this.investmentAccountRepository = investmentAccountRepository;
        this.brokerConnectionRepository = brokerConnectionRepository;
        this.holdingSummaryPort = holdingSummaryPort;
        this.holdingDetailPort = holdingDetailPort;
    }

    public ConnectionAccountsResult getAccountsByConnection(GetConnectionAccountsQuery query) {
        BrokerConnection connection = brokerConnectionRepository.findByIdAndUserId(query.connectionId(), query.userId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.CONNECTION_NOT_FOUND));

        List<ConnectionAccountsResult.AccountSummary> accounts = investmentAccountRepository
                .findByConnectionId(query.connectionId()).stream()
                .map(account -> toAccountSummary(query.userId(), account))
                .collect(Collectors.toList());

        return new ConnectionAccountsResult(
                connection.getConnectionId(), connection.getBrokerId(), connection.getBrokerName(), accounts);
    }

    public AccountListResult getAccounts(Long userId) {
        List<InvestmentAccount> accounts = investmentAccountRepository.findByUserId(userId);
        if (accounts.isEmpty()) {
            return AccountListResult.empty();
        }

        Map<Long, BrokerConnection> connectionsById = brokerConnectionRepository
                .findByIds(accounts.stream().map(InvestmentAccount::getConnectionId).distinct().collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(BrokerConnection::getConnectionId, Function.identity()));

        List<AccountListResult.AccountResult> accountResults = accounts.stream()
                .map(account -> toAccountResult(userId, account, connectionsById.get(account.getConnectionId())))
                .collect(Collectors.toList());

        BigDecimal totalMarketValue = sum(accountResults, AccountListResult.AccountResult::totalMarketValue);
        BigDecimal totalUnrealizedPnl = sum(accountResults, AccountListResult.AccountResult::totalUnrealizedPnl);
        AccountListResult.AccountsSummary summary = new AccountListResult.AccountsSummary(
                accountResults.size(), totalMarketValue, totalUnrealizedPnl);

        return new AccountListResult(summary, accountResults);
    }

    public AccountDetailResult getAccountDetail(GetAccountDetailQuery query) {
        InvestmentAccount account = investmentAccountRepository.findByIdAndUserId(query.accountId(), query.userId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.ACCOUNT_NOT_FOUND));
        BrokerConnection connection = brokerConnectionRepository.findByIds(List.of(account.getConnectionId())).get(0);

        AccountHoldingsInfo holdingsInfo = holdingDetailPort.getHoldings(query.userId(), account.getAccountId());
        AccountDetailResult.Summary summary = new AccountDetailResult.Summary(
                holdingsInfo.summary().holdingCount(),
                holdingsInfo.summary().totalMarketValue(),
                holdingsInfo.summary().totalUnrealizedPnl());
        List<AccountDetailResult.HoldingDetail> holdings = holdingsInfo.holdings().stream()
                .map(this::toHoldingDetail)
                .collect(Collectors.toList());

        return new AccountDetailResult(
                account.getAccountId(),
                account.getConnectionId(),
                connection.getBrokerId(),
                connection.getBrokerName(),
                account.getAccountNoMasked(),
                account.getAccountName(),
                account.getAccountType(),
                connection.getLastSyncedAt(),
                summary,
                holdings
        );
    }

    private ConnectionAccountsResult.AccountSummary toAccountSummary(Long userId, InvestmentAccount account) {
        HoldingSummaryInfo summary = holdingSummaryPort.summarize(userId, account.getAccountId());
        return new ConnectionAccountsResult.AccountSummary(
                account.getAccountId(),
                account.getAccountNoMasked(),
                account.getAccountName(),
                account.getAccountType(),
                summary.holdingCount(),
                summary.totalMarketValue(),
                summary.totalUnrealizedPnl()
        );
    }

    private AccountListResult.AccountResult toAccountResult(Long userId, InvestmentAccount account, BrokerConnection connection) {
        HoldingSummaryInfo summary = holdingSummaryPort.summarize(userId, account.getAccountId());
        return new AccountListResult.AccountResult(
                account.getAccountId(),
                account.getConnectionId(),
                connection.getBrokerId(),
                connection.getBrokerName(),
                account.getAccountNoMasked(),
                account.getAccountName(),
                account.getAccountType(),
                summary.holdingCount(),
                summary.totalMarketValue(),
                summary.totalUnrealizedPnl(),
                connection.getLastSyncedAt()
        );
    }

    private AccountDetailResult.HoldingDetail toHoldingDetail(HoldingDetailInfo info) {
        return new AccountDetailResult.HoldingDetail(
                info.securityId(),
                info.securityCode(),
                info.securityName(),
                info.marketType(),
                info.quantity(),
                info.averageCost(),
                info.marketValue(),
                info.unrealizedPnl(),
                info.portfolioWeight(),
                info.snapshotDate()
        );
    }

    private BigDecimal sum(List<AccountListResult.AccountResult> accounts, Function<AccountListResult.AccountResult, BigDecimal> extractor) {
        return accounts.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
