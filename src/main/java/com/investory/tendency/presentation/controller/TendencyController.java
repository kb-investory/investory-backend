package com.investory.tendency.presentation.controller;

import com.investory.tendency.domain.services.*;
import com.investory.tendency.domain.services.dto.command.AnalyzeRationaleTendencyQuery;
import com.investory.tendency.domain.services.dto.command.RunAnalysisCommand;
import com.investory.tendency.domain.services.dto.query.AnalyzeGainResponseQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzeHoldingPeriodQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzeLossResponseQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzePortfolioRiskQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzePrincipleAdherenceQuery;
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunDetailQuery;
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunsQuery;
import com.investory.tendency.domain.services.dto.result.*;
import com.investory.tendency.presentation.dto.response.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tendency")
public class TendencyController {

    private final LossResponseAnalysisService lossResponseAnalysisService;
    private final GainResponseAnalysisService gainResponseAnalysisService;
    private final PortfolioRiskAnalysisService portfolioRiskAnalysisService;
    private final RationaleTendencyService rationaleTendencyService;
    private final HoldingPeriodAnalysisService holdingPeriodAnalysisService;
    private final PrincipleAdherenceAnalysisService principleAdherenceAnalysisService;
    private final AnalysisRunService analysisRunService;


    public TendencyController(LossResponseAnalysisService lossResponseAnalysisService,
                              GainResponseAnalysisService gainResponseAnalysisService,
                              PortfolioRiskAnalysisService portfolioRiskAnalysisService,
                              RationaleTendencyService rationaleTendencyService,
                              HoldingPeriodAnalysisService holdingPeriodAnalysisService,
                              PrincipleAdherenceAnalysisService principleAdherenceAnalysisService,
                              AnalysisRunService analysisRunService) {
        this.lossResponseAnalysisService = lossResponseAnalysisService;
        this.gainResponseAnalysisService = gainResponseAnalysisService;
        this.portfolioRiskAnalysisService = portfolioRiskAnalysisService;
        this.rationaleTendencyService = rationaleTendencyService;
        this.holdingPeriodAnalysisService = holdingPeriodAnalysisService;
        this.principleAdherenceAnalysisService = principleAdherenceAnalysisService;
        this.analysisRunService = analysisRunService;

    }

    // 성향 분석 실행 — 6개 항목을 한 번에 계산해 저장하고, 그 결과(근거 포함)를 바로 반환한다.
    @PostMapping("/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisRunDetailResponse runAnalysis(@AuthenticationPrincipal Long userId) {
        AnalysisRunDetailResult result = analysisRunService.runAnalysis(new RunAnalysisCommand(userId));
        return AnalysisRunDetailResponse.from(result);
    }

    // 성향 분석 이력 목록 조회
    @GetMapping("/analyses")
    public AnalysisRunListResponse getAnalysisRuns(@AuthenticationPrincipal Long userId) {
        return AnalysisRunListResponse.from(analysisRunService.getHistory(new GetAnalysisRunsQuery(userId)));
    }

    // 사용자 투자 성향 분석 결과 및 근거 조회
    @GetMapping("/analyses/{analysisRunId}")
    public AnalysisRunDetailResponse getAnalysisRunDetail(
            @AuthenticationPrincipal Long userId, @PathVariable Long analysisRunId) {
        AnalysisRunDetailResult result = analysisRunService.getDetail(new GetAnalysisRunDetailQuery(userId, analysisRunId));
        return AnalysisRunDetailResponse.from(result);
    }

    @GetMapping("/loss-response")
    public LossResponseAnalysisResponse analyzeLossResponse(
            @AuthenticationPrincipal Long userId, @RequestParam Long securityId) {
        LossResponseAnalysisResult result = lossResponseAnalysisService.analyze(
                new AnalyzeLossResponseQuery(userId, securityId));
        return LossResponseAnalysisResponse.from(result);
    }

    @GetMapping("/gain-response")
    public GainResponseAnalysisResponse analyzeGainResponse(
            @AuthenticationPrincipal Long userId, @RequestParam Long securityId) {
        GainResponseAnalysisResult result = gainResponseAnalysisService.analyze(
                new AnalyzeGainResponseQuery(userId, securityId));
        return GainResponseAnalysisResponse.from(result);
    }

    @GetMapping("/portfolio-risk")
    public PortfolioRiskAnalysisResponse analyzePortfolioRisk(@AuthenticationPrincipal Long userId) {
        PortfolioRiskAnalysisResult result = portfolioRiskAnalysisService.analyze(
                new AnalyzePortfolioRiskQuery(userId));
        return PortfolioRiskAnalysisResponse.from(result);
    }

    // 최근 90일 매수 판단 근거(rationale_label) 집계 기반 투자 성향 분석.
    @GetMapping("/rational")
    public RationaleTendencyResponse analyzeRational(@AuthenticationPrincipal Long userId) {
        RationaleTendencyResult result = rationaleTendencyService.analyze(new AnalyzeRationaleTendencyQuery(userId));
        return RationaleTendencyResponse.from(result);
    }


    // 최근 90일 trade_matches.holding_days 분포 기반 투자 기간 성향 분석.
    @GetMapping("/holding-period")
    public HoldingPeriodAnalysisResponse analyzeHoldingPeriod(@AuthenticationPrincipal Long userId) {
        HoldingPeriodAnalysisResult result = holdingPeriodAnalysisService.analyze(new AnalyzeHoldingPeriodQuery(userId));
        return HoldingPeriodAnalysisResponse.from(result);
    }

    // 활성 원칙 세트와 실제 매매 행동 비교 기반 원칙 이행 성향 분석.
    @GetMapping("/principle-adherence")
    public PrincipleAdherenceAnalysisResponse analyzePrincipleAdherence(@AuthenticationPrincipal Long userId) {
        PrincipleAdherenceAnalysisResult result = principleAdherenceAnalysisService.analyze(
                new AnalyzePrincipleAdherenceQuery(userId));
        return PrincipleAdherenceAnalysisResponse.from(result);
    }
}
