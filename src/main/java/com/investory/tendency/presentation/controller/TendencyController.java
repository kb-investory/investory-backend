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

    // TODO: JWT 인증 도입 후 Principal.userId로 교체 (auth 필터 미적용으로 임시 고정값 사용)
    private static final Long TEMP_USER_ID = 1L;

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
    public AnalysisRunDetailResponse runAnalysis() {
        AnalysisRunDetailResult result = analysisRunService.runAnalysis(new RunAnalysisCommand(TEMP_USER_ID));
        return AnalysisRunDetailResponse.from(result);
    }

    // 성향 분석 이력 목록 조회
    @GetMapping("/analyses")
    public AnalysisRunListResponse getAnalysisRuns() {
        return AnalysisRunListResponse.from(analysisRunService.getHistory(new GetAnalysisRunsQuery(TEMP_USER_ID)));
    }

    // 사용자 투자 성향 분석 결과 및 근거 조회
    @GetMapping("/analyses/{analysisRunId}")
    public AnalysisRunDetailResponse getAnalysisRunDetail(@PathVariable Long analysisRunId) {
        AnalysisRunDetailResult result = analysisRunService.getDetail(new GetAnalysisRunDetailQuery(TEMP_USER_ID, analysisRunId));
        return AnalysisRunDetailResponse.from(result);
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
    public RationaleTendencyResponse analyzeRational() {
        RationaleTendencyResult result = rationaleTendencyService.analyze(new AnalyzeRationaleTendencyQuery(TEMP_USER_ID));
        return RationaleTendencyResponse.from(result);
    }


    // 최근 90일 trade_matches.holding_days 분포 기반 투자 기간 성향 분석.
    @GetMapping("/holding-period")
    public HoldingPeriodAnalysisResponse analyzeHoldingPeriod() {
        HoldingPeriodAnalysisResult result = holdingPeriodAnalysisService.analyze(new AnalyzeHoldingPeriodQuery(TEMP_USER_ID));
        return HoldingPeriodAnalysisResponse.from(result);
    }

    // 활성 원칙 세트와 실제 매매 행동 비교 기반 원칙 이행 성향 분석.
    @GetMapping("/principle-adherence")
    public PrincipleAdherenceAnalysisResponse analyzePrincipleAdherence() {
        PrincipleAdherenceAnalysisResult result = principleAdherenceAnalysisService.analyze(
                new AnalyzePrincipleAdherenceQuery(TEMP_USER_ID));
        return PrincipleAdherenceAnalysisResponse.from(result);
    }
}
