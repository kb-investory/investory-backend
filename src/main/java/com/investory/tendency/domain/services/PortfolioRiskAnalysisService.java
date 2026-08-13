package com.investory.tendency.domain.services;

import com.investory.tendency.domain.constant.ConcentrationLevel;
import com.investory.tendency.domain.constant.PortfolioRiskType;
import com.investory.tendency.domain.constant.VolatilityLevel;
import com.investory.tendency.domain.exception.TendencyErrorCode;
import com.investory.tendency.domain.exception.TendencyException;
import com.investory.tendency.domain.ports.HoldingSummaryPort;
import com.investory.tendency.domain.ports.MarketDataPort;
import com.investory.tendency.domain.ports.dto.DailyPriceInfo;
import com.investory.tendency.domain.ports.dto.HoldingWeightInfo;
import com.investory.tendency.domain.services.dto.query.AnalyzePortfolioRiskQuery;
import com.investory.tendency.domain.services.dto.result.PortfolioRiskAnalysisResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

// 1번(포트폴리오 위험배분) 분석. 3/4번과 달리 90일 day-walk가 아니라 "현재 보유 스냅샷" 기준.
// 분산/집중(종목 최대 비중) × 저변동/고변동(가중평균 일간등락률 표준편차) 2x2 매트릭스로 라벨링한다.
@Service
public class PortfolioRiskAnalysisService {

    private static final int VOLATILITY_WINDOW_DAYS = 90;
    private static final BigDecimal CONCENTRATION_THRESHOLD = BigDecimal.valueOf(40); // 최대 종목 비중 40% 기준
    // TODO: 실데이터 기반 튜닝 필요. 임시 시작값(가중평균 일간등락률 표준편차, %).
    private static final BigDecimal VOLATILITY_THRESHOLD = BigDecimal.valueOf(2);

    private final HoldingSummaryPort holdingSummaryPort;
    private final MarketDataPort marketDataPort;

    public PortfolioRiskAnalysisService(HoldingSummaryPort holdingSummaryPort, MarketDataPort marketDataPort) {
        this.holdingSummaryPort = holdingSummaryPort;
        this.marketDataPort = marketDataPort;
    }

    public PortfolioRiskAnalysisResult analyze(AnalyzePortfolioRiskQuery query) {
        List<HoldingWeightInfo> holdings = holdingSummaryPort.findHoldingWeights(query.userId());
        if (holdings.isEmpty()) {
            throw new TendencyException(TendencyErrorCode.INSUFFICIENT_HOLDING_DATA);
        }

        BigDecimal maxWeight = holdings.stream()
                .map(HoldingWeightInfo::portfolioWeight)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        ConcentrationLevel concentration = maxWeight.compareTo(CONCENTRATION_THRESHOLD) >= 0
                ? ConcentrationLevel.CONCENTRATED
                : ConcentrationLevel.DIVERSIFIED;

        BigDecimal weightedVolatility = computeWeightedVolatility(holdings);
        VolatilityLevel volatility = weightedVolatility.compareTo(VOLATILITY_THRESHOLD) >= 0
                ? VolatilityLevel.HIGH
                : VolatilityLevel.LOW;

        PortfolioRiskType type = classify(concentration, volatility);
        return new PortfolioRiskAnalysisResult(maxWeight, concentration, weightedVolatility, volatility, type);
    }

    // 가중평균 변동성 = Σ(weight_i * stdev_i) / Σ(weight_i) — 변동성 산출이 불가능한 종목(시세 데이터 부족)은 분모/분자 모두에서 제외.
    private BigDecimal computeWeightedVolatility(List<HoldingWeightInfo> holdings) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(VOLATILITY_WINDOW_DAYS - 1L);

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (HoldingWeightInfo holding : holdings) {
            BigDecimal stdev = computeReturnStdev(holding.securityId(), windowStart, today);
            if (stdev == null) {
                continue;
            }
            weightedSum = weightedSum.add(holding.portfolioWeight().multiply(stdev));
            totalWeight = totalWeight.add(holding.portfolioWeight());
        }
        if (totalWeight.signum() == 0) {
            return BigDecimal.ZERO; // 변동성 산출 가능한 종목이 하나도 없으면 저변동으로 취급
        }
        return weightedSum.divide(totalWeight, MathContext.DECIMAL64);
    }

    // 종목 하나의 일간등락률(dailyReturnRate) 모표준편차(%). 유효 데이터 2건 미만이면 null(산출 불가).
    private BigDecimal computeReturnStdev(Long securityId, LocalDate from, LocalDate to) {
        List<BigDecimal> returns = marketDataPort.findDailyPrices(securityId, from, to).stream()
                .map(DailyPriceInfo::dailyReturnRate)
                .filter(Objects::nonNull)
                .toList();
        if (returns.size() < 2) {
            return null;
        }
        BigDecimal mean = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), MathContext.DECIMAL64);
        BigDecimal sumSquaredDiff = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(returns.size()), MathContext.DECIMAL64);
        return variance.sqrt(MathContext.DECIMAL64);
    }

    private PortfolioRiskType classify(ConcentrationLevel concentration, VolatilityLevel volatility) {
        if (concentration == ConcentrationLevel.DIVERSIFIED && volatility == VolatilityLevel.LOW) {
            return PortfolioRiskType.LOW_VOLATILITY_DIVERSIFIED;
        }
        if (concentration == ConcentrationLevel.DIVERSIFIED && volatility == VolatilityLevel.HIGH) {
            return PortfolioRiskType.HIGH_VOLATILITY_DIVERSIFIED;
        }
        if (concentration == ConcentrationLevel.CONCENTRATED && volatility == VolatilityLevel.LOW) {
            return PortfolioRiskType.LOW_VOLATILITY_CONCENTRATED;
        }
        return PortfolioRiskType.HIGH_VOLATILITY_CONCENTRATED;
    }
}
