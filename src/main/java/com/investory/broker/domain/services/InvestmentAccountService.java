package com.investory.broker.domain.services;

import com.investory.broker.domain.exception.BrokerErrorCode;
import com.investory.broker.domain.exception.BrokerException;
import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.ports.HoldingSummaryPort;
import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.domain.services.dto.query.GetConnectionAccountsQuery;
import com.investory.broker.domain.services.dto.result.ConnectionAccountsResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvestmentAccountService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final BrokerConnectionRepository brokerConnectionRepository;
    private final HoldingSummaryPort holdingSummaryPort;

    public InvestmentAccountService(
            InvestmentAccountRepository investmentAccountRepository,
            BrokerConnectionRepository brokerConnectionRepository,
            HoldingSummaryPort holdingSummaryPort) {
        this.investmentAccountRepository = investmentAccountRepository;
        this.brokerConnectionRepository = brokerConnectionRepository;
        this.holdingSummaryPort = holdingSummaryPort;
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
}
