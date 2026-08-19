package com.investory.tendency.presentation.controller;

import com.investory.tendency.domain.services.AnalysisRunService;
import com.investory.tendency.domain.services.dto.command.RunAnalysisCommand;
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunDetailQuery;
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunsQuery;
import com.investory.tendency.domain.services.dto.result.AnalysisRunDetailResult;
import com.investory.tendency.presentation.dto.response.AnalysisRunDetailResponse;
import com.investory.tendency.presentation.dto.response.AnalysisRunListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tendency")
public class TendencyController {

    private final AnalysisRunService analysisRunService;

    public TendencyController(AnalysisRunService analysisRunService) {
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
}
