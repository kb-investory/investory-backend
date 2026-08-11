package com.investory.tendency.domain.services;

import com.investory.tendency.domain.exception.TendencyErrorCode;
import com.investory.tendency.domain.exception.TendencyException;
import com.investory.tendency.domain.constant.RationaleLabelType;
import com.investory.tendency.domain.constant.RationaleTendencyResultType;
import com.investory.tendency.domain.ports.RationaleLabelStatsPort;
import com.investory.tendency.domain.services.dto.command.AnalyzeRationaleTendencyQuery;
import com.investory.tendency.domain.services.dto.result.RationaleTendencyResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

// 최근 90일간의 journal_trade_notes.rationale_label만 집계해서 투자 판단 성향을 계산한다.
// LLM을 호출하지 않고, rationale_text도 사용하지 않는다 — 이미 DB에 저장된 rationale_label을 그대로 신뢰한다.
@Service
public class RationaleTendencyService {

    private static final int ANALYSIS_PERIOD_DAYS = 90;
    private static final double THRESHOLD = 60.0;

    private final RationaleLabelStatsPort rationaleLabelStatsPort;

    public RationaleTendencyService(RationaleLabelStatsPort rationaleLabelStatsPort) {
        this.rationaleLabelStatsPort = rationaleLabelStatsPort;
    }

    public RationaleTendencyResult analyze(AnalyzeRationaleTendencyQuery query) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(ANALYSIS_PERIOD_DAYS);

        Map<RationaleLabelType, Long> counts = rationaleLabelStatsPort
                .countByUserAndDateRange(query.userId(), startDate, endDate);

        long fundamentalCount = counts.getOrDefault(RationaleLabelType.FUNDAMENTAL_ANALYSIS, 0L);
        long priceTrendCount = counts.getOrDefault(RationaleLabelType.PRICE_TREND, 0L);
        long eventCount = counts.getOrDefault(RationaleLabelType.EVENT_REACTION, 0L);
        long intuitionCount = counts.getOrDefault(RationaleLabelType.INTUITION_SOCIAL_SIGNAL, 0L);
        long unclassifiedCount = counts.getOrDefault(RationaleLabelType.UNCLASSIFIED, 0L);

        // UNCLASSIFIED를 제외한 4개 유형의 합계 — 주요 4개 유형 비율의 분모.
        long validTotal = fundamentalCount + priceTrendCount + eventCount + intuitionCount;
        // 5개 유형 전체 합계 — UNCLASSIFIED 비율의 분모이자, "데이터가 아예 없는지" 판단 기준.
        long total = validTotal + unclassifiedCount;

        // 최근 90일 동안 판단 근거가 하나도 없으면 특정 유형을 임의로 반환하지 않고 명시적으로 실패시킨다.
        if (total == 0) {
            throw new TendencyException(TendencyErrorCode.INSUFFICIENT_TRADE_DATA);
        }

        double fundamentalRatio = ratio(fundamentalCount, validTotal);
        double priceTrendRatio = ratio(priceTrendCount, validTotal);
        double eventRatio = ratio(eventCount, validTotal);
        double intuitionRatio = ratio(intuitionCount, validTotal);
        double unclassifiedRatio = ratio(unclassifiedCount, total);

        RationaleTendencyResultType result = decideResult(fundamentalRatio, priceTrendRatio, eventRatio, intuitionRatio);

        return new RationaleTendencyResult(
                round(fundamentalRatio),
                round(priceTrendRatio),
                round(eventRatio),
                round(intuitionRatio),
                round(unclassifiedRatio),
                THRESHOLD,
                result
        );
    }

    private double ratio(long count, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return (double) count / denominator * 100;
    }

    // Threshold 비교는 반올림한 값이 아니라 반올림 전 실제 계산값을 기준으로 한다(예: 59.9999%는 60%로 반올림해서
    // 통과시키지 않는다). 4개 유형 비율이 동률이어도 Threshold(60%) 미만이면 결과가 무조건 COMPLEX이므로
    // 동률 상황에서 4개 유형 중 하나를 임의로 골라야 하는 경우는 생기지 않는다.
    private RationaleTendencyResultType decideResult(double fundamentalRatio, double priceTrendRatio,
                                                     double eventRatio, double intuitionRatio) {
        double maxRatio = Math.max(Math.max(fundamentalRatio, priceTrendRatio), Math.max(eventRatio, intuitionRatio));

        if (maxRatio < THRESHOLD) {
            return RationaleTendencyResultType.COMPLEX;
        }
        if (fundamentalRatio == maxRatio) {
            return RationaleTendencyResultType.FUNDAMENTAL_ANALYSIS;
        }
        if (priceTrendRatio == maxRatio) {
            return RationaleTendencyResultType.PRICE_TREND;
        }
        if (eventRatio == maxRatio) {
            return RationaleTendencyResultType.EVENT_REACTION;
        }
        return RationaleTendencyResultType.INTUITION_SOCIAL_SIGNAL;
    }

    // 응답용 반올림(소수점 둘째 자리) — Threshold 판정 이후, 결과 DTO를 만들 때만 적용한다.
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
