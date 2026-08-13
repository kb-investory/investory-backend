package com.investory.journal.infra.port_impls;

import com.investory.journal.domain.constant.TradeSide;
import com.investory.journal.domain.ports.TradeLedgerPort;
import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;
import com.investory.journal.domain.ports.dto.TradeTimelineInfo;
import com.investory.ledger.domain.services.TradeQueryService;
import com.investory.ledger.domain.services.dto.query.GetTradesQuery;
import com.investory.ledger.domain.services.dto.result.TradeListResult;
import com.investory.ledger.domain.services.dto.result.TradeResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TradeLedgerPortImpl implements TradeLedgerPort {

    // ledger.TradeQueryService의 페이지 크기 상한과 동일 (전체 이력을 다 받을 때까지 이 크기로 반복 조회)
    private static final int PAGE_SIZE = 100;

    // 투자일지의 날짜별 집계는 사용자 기준 시간대(KST)로 판단한다 — UTC 자정 근처 거래가
    // 엉뚱한 날짜로 집계되는 걸 막기 위함.
    private static final ZoneId JOURNAL_ZONE = ZoneId.of("Asia/Seoul");

    private final TradeQueryService tradeQueryService;

    public TradeLedgerPortImpl(TradeQueryService tradeQueryService) {
        this.tradeQueryService = tradeQueryService;
    }

    @Override
    public List<TradeCountInfo> countTradesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> countsByDate = fetchAllTrades(userId, null, startDate, endDate).stream()
                .collect(Collectors.groupingBy(this::tradedOnDate, Collectors.counting()));

        return countsByDate.entrySet().stream()
                .map(entry -> new TradeCountInfo(entry.getKey(), entry.getValue().intValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TradeInfo> findTradesOn(Long userId, LocalDate date) {
        return fetchAllTrades(userId, null, date, date).stream()
                .map(this::toTradeInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<TradeTimelineInfo> findTradesBySecurity(Long userId, Long securityId, LocalDate startDate, LocalDate endDate, int page, int size) {
        GetTradesQuery query = new GetTradesQuery(userId, null, securityId, null, startDate, endDate, page, size);
        return tradeQueryService.getTrades(query).content().stream()
                .map(this::toTradeTimelineInfo)
                .collect(Collectors.toList());
    }

    @Override
    public long countTradesBySecurity(Long userId, Long securityId, LocalDate startDate, LocalDate endDate) {
        // content는 필요 없고 totalElements만 필요해서 최소 크기(1건)로 조회한다.
        GetTradesQuery query = new GetTradesQuery(userId, null, securityId, null, startDate, endDate, 0, 1);
        return tradeQueryService.getTrades(query).totalElements();
    }

    private List<TradeResult> fetchAllTrades(Long userId, Long securityId, LocalDate startDate, LocalDate endDate) {
        List<TradeResult> allTrades = new ArrayList<>();
        int page = 0;
        while (true) {
            GetTradesQuery query = new GetTradesQuery(userId, null, securityId, null, startDate, endDate, page, PAGE_SIZE);
            TradeListResult result = tradeQueryService.getTrades(query);
            allTrades.addAll(result.content());
            if (!result.hasNext()) {
                break;
            }
            page++;
        }
        return allTrades;
    }

    private LocalDate tradedOnDate(TradeResult trade) {
        return LocalDate.ofInstant(trade.tradedAt(), JOURNAL_ZONE);
    }

    private TradeInfo toTradeInfo(TradeResult trade) {
        return new TradeInfo(
                trade.tradeId(),
                trade.securityId(),
                TradeSide.valueOf(trade.tradeSide().name()),
                trade.quantity().intValueExact(),
                trade.unitPrice(),
                trade.tradedAt()
        );
    }

    private TradeTimelineInfo toTradeTimelineInfo(TradeResult trade) {
        return new TradeTimelineInfo(
                trade.tradeId(),
                trade.accountId(),
                trade.accountName(),
                TradeSide.valueOf(trade.tradeSide().name()),
                trade.quantity().intValueExact(),
                trade.unitPrice(),
                trade.transactionCostAmount(),
                trade.tradedAt()
        );
    }
}
