package com.investory.tendency.domain.services;

import com.investory.tendency.domain.exception.TendencyErrorCode;
import com.investory.tendency.domain.exception.TendencyException;
import com.investory.tendency.domain.constant.HoldingPeriodType;
import com.investory.tendency.domain.ports.TradeMatchQueryPort;
import com.investory.tendency.domain.services.dto.query.AnalyzeHoldingPeriodQuery;
import com.investory.tendency.domain.services.dto.result.HoldingPeriodAnalysisResult;
import com.investory.tendency.domain.services.dto.result.HoldingPeriodBucketResult;
import org.springframework.stereotype.Service;

import java.util.List;

// 최근 90일 trade_matches.holding_days 분포를 구간별로 집계해서 투자 기간 성향을 계산한다.
// holding_days는 trade_matches에 매칭 시점에 이미 계산되어 저장된 값을 그대로 신뢰한다 — 여기서 다시 계산하지 않는다.
// LLM을 호출하지 않고, 순수 Java 로직으로 deterministic하게 판단한다.
@Service
public class HoldingPeriodAnalysisService {

    private static final int SHORT_TERM_MAX_DAYS = 5;
    private static final int MEDIUM_TERM_MAX_DAYS = 30;
    private static final double THRESHOLD = 0.6;

    private final TradeMatchQueryPort tradeMatchQueryPort;

    public HoldingPeriodAnalysisService(TradeMatchQueryPort tradeMatchQueryPort) {
        this.tradeMatchQueryPort = tradeMatchQueryPort;
    }

    public HoldingPeriodAnalysisResult analyze(AnalyzeHoldingPeriodQuery query) {
        List<Integer> holdingDaysList = tradeMatchQueryPort.findHoldingDaysForLast90Days(query.userId());

        // 최근 90일 동안 매칭된 거래가 하나도 없으면 특정 성향을 임의로 반환하지 않고 명시적으로 실패시킨다.
        // (MIXED는 데이터가 있는데 어느 구간도 threshold를 못 넘겼을 때만 쓰는 값이라 데이터 부재와 구분해야 한다)
        int totalCount = holdingDaysList.size();
        if (totalCount == 0) {
            throw new TendencyException(TendencyErrorCode.INSUFFICIENT_TRADE_DATA);
        }

        int shortTermCount = 0;
        int mediumTermCount = 0;
        int longTermCount = 0;
        for (int holdingDays : holdingDaysList) {
            if (holdingDays <= SHORT_TERM_MAX_DAYS) {
                shortTermCount++;
            } else if (holdingDays <= MEDIUM_TERM_MAX_DAYS) {
                mediumTermCount++;
            } else {
                longTermCount++;
            }
        }

        double shortTermRatio = ratio(shortTermCount, totalCount);
        double mediumTermRatio = ratio(mediumTermCount, totalCount);
        double longTermRatio = ratio(longTermCount, totalCount);

        HoldingPeriodType type = decideType(shortTermRatio, mediumTermRatio, longTermRatio);

        return new HoldingPeriodAnalysisResult(
                type,
                THRESHOLD,
                totalCount,
                new HoldingPeriodBucketResult(shortTermCount, round(shortTermRatio)),
                new HoldingPeriodBucketResult(mediumTermCount, round(mediumTermRatio)),
                new HoldingPeriodBucketResult(longTermCount, round(longTermRatio))
        );
    }

    // Threshold 비교는 반올림한 값이 아니라 반올림 전 실제 계산값을 기준으로 한다.
    private HoldingPeriodType decideType(double shortTermRatio, double mediumTermRatio, double longTermRatio) {
        if (shortTermRatio >= THRESHOLD) {
            return HoldingPeriodType.SHORT_TERM;
        }
        if (mediumTermRatio >= THRESHOLD) {
            return HoldingPeriodType.MEDIUM_TERM;
        }
        if (longTermRatio >= THRESHOLD) {
            return HoldingPeriodType.LONG_TERM;
        }
        return HoldingPeriodType.MIXED;
    }

    private double ratio(int count, int total) {
        return (double) count / total;
    }

    // 응답용 반올림(소수점 둘째 자리) — Threshold 판정 이후, 결과 DTO를 만들 때만 적용한다.
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
