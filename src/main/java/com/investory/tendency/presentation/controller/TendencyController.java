package com.investory.tendency.presentation.controller;

import com.investory.tendency.domain.services.GainResponseAnalysisService;
import com.investory.tendency.domain.services.LossResponseAnalysisService;
import com.investory.tendency.domain.services.PortfolioRiskAnalysisService;
import com.investory.tendency.domain.services.RationaleTendencyService;
import com.investory.tendency.domain.services.dto.command.AnalyzeRationaleTendencyQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzeGainResponseQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzeLossResponseQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzePortfolioRiskQuery;
import com.investory.tendency.domain.services.dto.result.GainResponseAnalysisResult;
import com.investory.tendency.domain.services.dto.result.LossResponseAnalysisResult;
import com.investory.tendency.domain.services.dto.result.PortfolioRiskAnalysisResult;
import com.investory.tendency.domain.services.dto.result.RationaleTendencyResult;
import com.investory.tendency.presentation.dto.response.GainResponseAnalysisResponse;
import com.investory.tendency.presentation.dto.response.LossResponseAnalysisResponse;
import com.investory.tendency.presentation.dto.response.PortfolioRiskAnalysisResponse;
import com.investory.tendency.presentation.dto.response.RationaleTendencyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tendency")
public class TendencyController {

    // TODO: JWT 인증 도입 후 Principal.userId로 교체 (auth 필터 미적용으로 임시 고정값 사용)
    private static final Long TEMP_USER_ID = 1L;

    private final LossResponseAnalysisService lossResponseAnalysisService;
    private final GainResponseAnalysisService gainResponseAnalysisService;
    private final PortfolioRiskAnalysisService portfolioRiskAnalysisService;
    private final RationaleTendencyService rationaleTendencyService;


    public TendencyController(LossResponseAnalysisService lossResponseAnalysisService,
                              GainResponseAnalysisService gainResponseAnalysisService,
                              PortfolioRiskAnalysisService portfolioRiskAnalysisService,
                              RationaleTendencyService rationaleTendencyService) {
        this.lossResponseAnalysisService = lossResponseAnalysisService;
        this.gainResponseAnalysisService = gainResponseAnalysisService;
        this.portfolioRiskAnalysisService = portfolioRiskAnalysisService;
        this.rationaleTendencyService = rationaleTendencyService;
    }

    @GetMapping("/loss-response")
    public LossResponseAnalysisResponse analyzeLossResponse(@RequestParam Long securityId) {
        LossResponseAnalysisResult result = lossResponseAnalysisService.analyze(
                new AnalyzeLossResponseQuery(TEMP_USER_ID, securityId));
        return LossResponseAnalysisResponse.from(result);
    }

    @GetMapping("/gain-response")
    public GainResponseAnalysisResponse analyzeGainResponse(@RequestParam Long securityId) {
        GainResponseAnalysisResult result = gainResponseAnalysisService.analyze(
                new AnalyzeGainResponseQuery(TEMP_USER_ID, securityId));
        return GainResponseAnalysisResponse.from(result);
    }

    @GetMapping("/portfolio-risk")
    public PortfolioRiskAnalysisResponse analyzePortfolioRisk() {
        PortfolioRiskAnalysisResult result = portfolioRiskAnalysisService.analyze(
                new AnalyzePortfolioRiskQuery(TEMP_USER_ID));
        return PortfolioRiskAnalysisResponse.from(result);
    }

    // 최근 90일 매수 판단 근거(rationale_label) 집계 기반 투자 성향 분석.
    @GetMapping("/rational")
    public RationaleTendencyResponse analyze() {
        RationaleTendencyResult result = rationaleTendencyService.analyze(new AnalyzeRationaleTendencyQuery(TEMP_USER_ID));
        return RationaleTendencyResponse.from(result);
    }
}
