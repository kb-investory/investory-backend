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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 손실 대응 성향(3번) 분석. 평단은 첫 매수부터 오늘까지 전체 거래를 누적해 계산하고,
// 손실/수익 판정과 행동 집계만 분석창(ANALYSIS_WINDOW_DAYS) 안으로 자른다 — 두 범위가 다르다는 점이 핵심.
// 일별 평단/포지션 계산은 PositionDailyWalk·DailyPnlWalker, θ 다수결 라벨링은 ThresholdMajorityLabeler로
// 4번(수익 대응)과 공유한다.
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
        Map<LocalDate, BigDecimal> closePriceByDay = marketDataPort.findDailyPrices(query.securityId(), windowStart, today).stream()
                .collect(Collectors.toMap(DailyPriceInfo::priceDate, DailyPriceInfo::closePrice));

        List<DailyPnlWalker.DailyOutcome> outcomes = DailyPnlWalker.walk(trades, closePriceByDay, windowStart, today);

        int totalLossDays = 0;
        int netSellDays = 0;
        int netBuyDays = 0;
        int holdDays = 0;

        for (DailyPnlWalker.DailyOutcome outcome : outcomes) {
            if (outcome.pnl().signum() >= 0) {
                continue;
            }
            totalLossDays++;
            int sign = outcome.netTradeSign();
            if (sign < 0) {
                netSellDays++;
            } else if (sign > 0) {
                netBuyDays++;
            } else {
                holdDays++;
            }
        }

        LossResponseType type = classify(totalLossDays, netSellDays, netBuyDays, holdDays);
        return new LossResponseAnalysisResult(query.securityId(), totalLossDays, netSellDays, netBuyDays, holdDays, type);
    }

    private LossResponseType classify(int totalLossDays, int netSellDays, int netBuyDays, int holdDays) {
        List<ThresholdMajorityLabeler.Bucket<LossResponseType>> buckets = List.of(
                new ThresholdMajorityLabeler.Bucket<>(LossResponseType.STOP_LOSS, netSellDays),
                new ThresholdMajorityLabeler.Bucket<>(LossResponseType.AVERAGING_DOWN, netBuyDays),
                new ThresholdMajorityLabeler.Bucket<>(LossResponseType.HOLD, holdDays)
        );
        return ThresholdMajorityLabeler.classify(buckets, totalLossDays, THRESHOLD, LossResponseType.MIXED);
    }
}
