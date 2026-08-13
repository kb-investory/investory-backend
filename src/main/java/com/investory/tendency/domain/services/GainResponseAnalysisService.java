package com.investory.tendency.domain.services;

import com.investory.tendency.domain.constant.GainResponseType;
import com.investory.tendency.domain.exception.TendencyErrorCode;
import com.investory.tendency.domain.exception.TendencyException;
import com.investory.tendency.domain.ports.MarketDataPort;
import com.investory.tendency.domain.ports.TradeLedgerPort;
import com.investory.tendency.domain.ports.dto.DailyPriceInfo;
import com.investory.tendency.domain.ports.dto.TradeInfo;
import com.investory.tendency.domain.services.dto.query.AnalyzeGainResponseQuery;
import com.investory.tendency.domain.services.dto.result.GainResponseAnalysisResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 수익 대응 성향(4번) 분석. 3번(LossResponseAnalysisService)과 구조적으로 동일 —
// PositionDailyWalk/DailyPnlWalker/ThresholdMajorityLabeler를 그대로 공유하고,
// 유일한 차이는 손실일(pnl<0) 대신 수익일(pnl>0)을 필터링한다는 점과 결과 라벨 enum이다.
// 손익=0인 날은 3번·4번 양쪽 모두에서 제외되어 중복 집계되지 않는다.
@Service
public class GainResponseAnalysisService {

    private static final int ANALYSIS_WINDOW_DAYS = 90;
    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(60, 2);

    private final TradeLedgerPort tradeLedgerPort;
    private final MarketDataPort marketDataPort;

    public GainResponseAnalysisService(TradeLedgerPort tradeLedgerPort, MarketDataPort marketDataPort) {
        this.tradeLedgerPort = tradeLedgerPort;
        this.marketDataPort = marketDataPort;
    }

    public GainResponseAnalysisResult analyze(AnalyzeGainResponseQuery query) {
        List<TradeInfo> trades = tradeLedgerPort.findTrades(query.userId(), query.securityId());
        if (trades.isEmpty()) {
            throw new TendencyException(TendencyErrorCode.INSUFFICIENT_TRADE_DATA);
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(ANALYSIS_WINDOW_DAYS - 1L);
        Map<LocalDate, BigDecimal> closePriceByDay = marketDataPort.findDailyPrices(query.securityId(), windowStart, today).stream()
                .collect(Collectors.toMap(DailyPriceInfo::priceDate, DailyPriceInfo::closePrice));

        List<DailyPnlWalker.DailyOutcome> outcomes = DailyPnlWalker.walk(trades, closePriceByDay, windowStart, today);

        int totalGainDays = 0;
        int netSellDays = 0;
        int netBuyDays = 0;
        int holdDays = 0;

        for (DailyPnlWalker.DailyOutcome outcome : outcomes) {
            if (outcome.pnl().signum() <= 0) {
                continue;
            }
            totalGainDays++;
            int sign = outcome.netTradeSign();
            if (sign < 0) {
                netSellDays++;
            } else if (sign > 0) {
                netBuyDays++;
            } else {
                holdDays++;
            }
        }

        GainResponseType type = classify(totalGainDays, netSellDays, netBuyDays, holdDays);
        return new GainResponseAnalysisResult(query.securityId(), totalGainDays, netSellDays, netBuyDays, holdDays, type);
    }

    private GainResponseType classify(int totalGainDays, int netSellDays, int netBuyDays, int holdDays) {
        List<ThresholdMajorityLabeler.Bucket<GainResponseType>> buckets = List.of(
                new ThresholdMajorityLabeler.Bucket<>(GainResponseType.TAKE_PROFIT, netSellDays),
                new ThresholdMajorityLabeler.Bucket<>(GainResponseType.AVERAGING_UP, netBuyDays),
                new ThresholdMajorityLabeler.Bucket<>(GainResponseType.HOLD, holdDays)
        );
        return ThresholdMajorityLabeler.classify(buckets, totalGainDays, THRESHOLD, GainResponseType.MIXED);
    }
}
