package com.investory.tendency.domain.services;

import com.investory.tendency.domain.constant.LossResponseType;
import com.investory.tendency.domain.exception.TendencyErrorCode;
import com.investory.tendency.domain.exception.TendencyException;
import com.investory.tendency.domain.ports.MarketDataPort;
import com.investory.tendency.domain.ports.TradeLedgerPort;
import com.investory.tendency.domain.ports.dto.DailyPriceInfo;
import com.investory.tendency.domain.ports.dto.TradeInfo;
import com.investory.tendency.domain.services.dto.query.AnalyzeLossResponseQuery;
import com.investory.tendency.domain.services.dto.result.LossResponseAnalysisResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 손실 대응 성향(3번) 분석. 평단은 첫 매수부터 오늘까지 전체 거래를 누적해 계산하고,
// 손실/수익 판정과 행동 집계만 분석창(ANALYSIS_WINDOW_DAYS) 안으로 자른다 — 두 범위가 다르다는 점이 핵심.
@Service
public class LossResponseAnalysisService {

    private static final int ANALYSIS_WINDOW_DAYS = 90;

    // 최빈 행동 비율이 이 값 이상이어야 그 유형으로 확정, 미달이면 혼합대응형.
    // 데이터 보며 튜닝할 값이라 상수로 분리해둔다 (시작값 60%).
    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(60, 2);

    private final TradeLedgerPort tradeLedgerPort;
    private final MarketDataPort marketDataPort;

    public LossResponseAnalysisService(TradeLedgerPort tradeLedgerPort, MarketDataPort marketDataPort) {
        this.tradeLedgerPort = tradeLedgerPort;
        this.marketDataPort = marketDataPort;
    }

    public LossResponseAnalysisResult analyze(AnalyzeLossResponseQuery query) {
        List<TradeInfo> trades = tradeLedgerPort.findTrades(query.userId(), query.securityId());
        if (trades.isEmpty()) {
            throw new TendencyException(TendencyErrorCode.INSUFFICIENT_TRADE_DATA);
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(ANALYSIS_WINDOW_DAYS - 1L);
        LocalDate firstTradeDate = toDate(trades.get(0).tradedAt());

        Map<LocalDate, List<TradeInfo>> tradesByDay = trades.stream()
                .collect(Collectors.groupingBy(t -> toDate(t.tradedAt())));
        Map<LocalDate, BigDecimal> closePriceByDay = marketDataPort.findDailyPrices(query.securityId(), windowStart, today).stream()
                .collect(Collectors.toMap(DailyPriceInfo::priceDate, DailyPriceInfo::closePrice));

        DailyWalk walk = new DailyWalk();
        int totalLossDays = 0;
        int netSellDays = 0;
        int netBuyDays = 0;
        int holdDays = 0;

        for (LocalDate date = firstTradeDate; !date.isAfter(today); date = date.plusDays(1)) {
            BigDecimal preMarketQty = walk.quantity;
            DayResult day = walk.apply(tradesByDay.getOrDefault(date, List.of()));

            // 장 시작 시점에 보유량이 없던 날(첫 매수일 등)은 손익 판정 대상이 아니다
            if (preMarketQty.signum() <= 0) {
                continue;
            }
            // 평단 계산에는 분석창 밖의 과거 거래도 필요하지만, 손익 판정·집계는 분석창 안으로만 자른다
            if (date.isBefore(windowStart)) {
                continue;
            }

            BigDecimal pnl;
            if (day.fullyExited) {
                BigDecimal avgExitPrice = day.exitAmount.divide(day.exitQuantity, MathContext.DECIMAL64);
                pnl = avgExitPrice.subtract(day.avgCostForPnl);
            } else {
                BigDecimal closePrice = closePriceByDay.get(date);
                if (closePrice == null) {
                    continue; // 그날 시세가 없으면(비거래일 등) 판정 불가 — 건너뜀
                }
                pnl = closePrice.subtract(day.avgCostForPnl);
            }

            if (pnl.signum() >= 0) {
                continue;
            }
            totalLossDays++;
            int netQtySign = day.netQuantity.signum();
            if (netQtySign < 0) {
                netSellDays++;
            } else if (netQtySign > 0) {
                netBuyDays++;
            } else {
                holdDays++;
            }
        }

        LossResponseType type = classify(totalLossDays, netSellDays, netBuyDays, holdDays);
        return new LossResponseAnalysisResult(query.securityId(), totalLossDays, netSellDays, netBuyDays, holdDays, type);
    }

    private LossResponseType classify(int totalLossDays, int netSellDays, int netBuyDays, int holdDays) {
        if (totalLossDays == 0) {
            return LossResponseType.MIXED;
        }
        int max = Math.max(netSellDays, Math.max(netBuyDays, holdDays));
        BigDecimal ratio = BigDecimal.valueOf(max).divide(BigDecimal.valueOf(totalLossDays), MathContext.DECIMAL64);
        if (ratio.compareTo(THRESHOLD) < 0) {
            return LossResponseType.MIXED;
        }
        if (max == netSellDays) {
            return LossResponseType.STOP_LOSS;
        }
        if (max == netBuyDays) {
            return LossResponseType.AVERAGING_DOWN;
        }
        return LossResponseType.HOLD;
    }

    private LocalDate toDate(java.time.Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    // 하루치 거래를 순서대로 반영하며 누적 보유량/평단(평균법)을 이어간다.
    // 매도는 평단을 바꾸지 않는다 — 그래서 완전매각으로 보유량이 0이 되어도 avgCost는 그대로 남겨,
    // 그 매각의 손익 계산에 쓸 수 있게 한다 (다음 매수가 들어오면 그때 새로 계산됨).
    private static class DailyWalk {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal costBasis = BigDecimal.ZERO;
        private BigDecimal avgCost = BigDecimal.ZERO;

        DayResult apply(List<TradeInfo> dayTrades) {
            BigDecimal netQuantity = BigDecimal.ZERO;
            BigDecimal exitQuantity = BigDecimal.ZERO;
            BigDecimal exitAmount = BigDecimal.ZERO;

            for (TradeInfo trade : dayTrades) {
                if ("BUY".equals(trade.tradeSide())) {
                    costBasis = costBasis.add(trade.quantity().multiply(trade.unitPrice()));
                    quantity = quantity.add(trade.quantity());
                    avgCost = costBasis.divide(quantity, MathContext.DECIMAL64);
                    netQuantity = netQuantity.add(trade.quantity());
                } else {
                    costBasis = costBasis.subtract(avgCost.multiply(trade.quantity()));
                    quantity = quantity.subtract(trade.quantity());
                    exitQuantity = exitQuantity.add(trade.quantity());
                    exitAmount = exitAmount.add(trade.quantity().multiply(trade.unitPrice()));
                    netQuantity = netQuantity.subtract(trade.quantity());
                }
            }

            boolean fullyExited = quantity.signum() == 0 && exitQuantity.signum() > 0;
            return new DayResult(netQuantity, fullyExited, exitQuantity, exitAmount, avgCost);
        }
    }

    private record DayResult(
            BigDecimal netQuantity, boolean fullyExited,
            BigDecimal exitQuantity, BigDecimal exitAmount, BigDecimal avgCostForPnl) {
    }
}
