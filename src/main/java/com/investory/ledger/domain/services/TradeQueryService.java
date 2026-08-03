package com.investory.ledger.domain.services;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.exception.LedgerErrorCode;
import com.investory.ledger.domain.exception.LedgerException;
import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.ports.AccountPort;
import com.investory.ledger.domain.ports.MarketDataPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.TradeRepository;
import com.investory.ledger.domain.repositories.TradeSearchCriteria;
import com.investory.ledger.domain.services.dto.query.GetTradeDetailQuery;
import com.investory.ledger.domain.services.dto.query.GetTradesQuery;
import com.investory.ledger.domain.services.dto.result.AccountDetailResult;
import com.investory.ledger.domain.services.dto.result.SecurityDetailResult;
import com.investory.ledger.domain.services.dto.result.TradeDetailResult;
import com.investory.ledger.domain.services.dto.result.TradeListResult;
import com.investory.ledger.domain.services.dto.result.TradeResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TradeQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TradeRepository tradeRepository;
    private final AccountPort accountPort;
    private final MarketDataPort marketDataPort;

    public TradeQueryService(TradeRepository tradeRepository, AccountPort accountPort, MarketDataPort marketDataPort) {
        this.tradeRepository = tradeRepository;
        this.accountPort = accountPort;
        this.marketDataPort = marketDataPort;
    }

    public TradeListResult getTrades(GetTradesQuery query) {
        validatePage(query.page(), query.size());
        validateDateRange(query.from(), query.to());
        TradeSide tradeSide = parseTradeSide(query.tradeSide());

        List<Long> accountIds = resolveAccountIds(query.userId(), query.accountId());
        if (accountIds.isEmpty()) {
            return emptyResult(query.page(), query.size());
        }

        TradeSearchCriteria criteria = new TradeSearchCriteria(
                accountIds, query.securityId(), tradeSide, query.from(), query.to(), query.page(), query.size());

        List<Trade> trades = tradeRepository.search(criteria);
        long totalElements = tradeRepository.count(criteria);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / query.size());
        boolean hasNext = (long) (query.page() + 1) * query.size() < totalElements;

        return new TradeListResult(assembleTradeResults(trades), query.page(), query.size(), totalElements, totalPages, hasNext);
    }

    public TradeDetailResult getTradeDetail(GetTradeDetailQuery query) {
        Trade trade = tradeRepository.findById(query.tradeId())
                .orElseThrow(() -> new LedgerException(LedgerErrorCode.TRADE_NOT_FOUND));

        // 존재하지 않는 거래와 남의 거래를 구분 없이 동일하게 처리 (소유권 노출 방지)
        AccountInfo account = accountPort.findAccount(trade.getAccountId(), query.userId())
                .orElseThrow(() -> new LedgerException(LedgerErrorCode.TRADE_NOT_FOUND));

        SecurityInfo security = marketDataPort.findSecurities(List.of(trade.getSecurityId())).stream()
                .findFirst()
                .orElse(null);

        return new TradeDetailResult(
                trade.getTradeId(),
                new AccountDetailResult(account.accountId(), account.accountName(), account.accountNumberMasked(), account.brokerageName()),
                toSecurityDetailResult(security, trade.getSecurityId()),
                trade.getTradeSide(),
                trade.getQuantity(),
                trade.getUnitPrice(),
                trade.getTradeAmount(),
                trade.getTransactionCostAmount(),
                trade.getSettlementAmount(),
                trade.getTradedAt()
        );
    }

    private List<Long> resolveAccountIds(Long userId, Long accountId) {
        if (accountId != null) {
            AccountInfo account = accountPort.findAccount(accountId, userId)
                    .orElseThrow(() -> new LedgerException(LedgerErrorCode.ACCOUNT_NOT_FOUND));
            return List.of(account.accountId());
        }
        return accountPort.findAccountsByUserId(userId).stream()
                .map(AccountInfo::accountId)
                .collect(Collectors.toList());
    }

    private List<TradeResult> assembleTradeResults(List<Trade> trades) {
        if (trades.isEmpty()) {
            return List.of();
        }
        List<Long> accountIds = trades.stream().map(Trade::getAccountId).distinct().collect(Collectors.toList());
        List<Long> securityIds = trades.stream().map(Trade::getSecurityId).distinct().collect(Collectors.toList());

        Map<Long, AccountInfo> accountsById = accountPort.findAccounts(accountIds).stream()
                .collect(Collectors.toMap(AccountInfo::accountId, Function.identity()));
        Map<Long, SecurityInfo> securitiesById = marketDataPort.findSecurities(securityIds).stream()
                .collect(Collectors.toMap(SecurityInfo::securityId, Function.identity()));

        return trades.stream()
                .map(trade -> toTradeResult(trade, accountsById.get(trade.getAccountId()), securitiesById.get(trade.getSecurityId())))
                .collect(Collectors.toList());
    }

    private TradeResult toTradeResult(Trade trade, AccountInfo account, SecurityInfo security) {
        return new TradeResult(
                trade.getTradeId(),
                trade.getAccountId(),
                account != null ? account.accountName() : null,
                trade.getSecurityId(),
                security != null ? security.securityCode() : null,
                security != null ? security.securityName() : null,
                security != null ? security.marketType() : null,
                trade.getTradeSide(),
                trade.getQuantity(),
                trade.getUnitPrice(),
                trade.getTradeAmount(),
                trade.getTransactionCostAmount(),
                trade.getTradedAt()
        );
    }

    private SecurityDetailResult toSecurityDetailResult(SecurityInfo security, Long securityId) {
        if (security == null) {
            return new SecurityDetailResult(securityId, null, null, null, null);
        }
        return new SecurityDetailResult(security.securityId(), security.securityCode(), security.securityName(),
                security.marketType(), security.sectorName());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new LedgerException(LedgerErrorCode.LEDGER_INVALID_PAGE_REQUEST);
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new LedgerException(LedgerErrorCode.LEDGER_INVALID_DATE_RANGE);
        }
    }

    private TradeSide parseTradeSide(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TradeSide.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new LedgerException(LedgerErrorCode.LEDGER_INVALID_TRADE_SIDE);
        }
    }

    private TradeListResult emptyResult(int page, int size) {
        return new TradeListResult(List.of(), page, size, 0, 0, false);
    }
}
