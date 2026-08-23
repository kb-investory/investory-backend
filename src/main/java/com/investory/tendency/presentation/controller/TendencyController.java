package com.investory.tendency.presentation.controller;

import com.investory.tendency.domain.services.AnalysisRunService;
import com.investory.tendency.domain.services.dto.command.RunAnalysisCommand;
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunDetailQuery;
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunsQuery;
import com.investory.tendency.domain.services.dto.result.AnalysisRunAcceptedResult;
import com.investory.tendency.domain.services.dto.result.AnalysisRunDetailResult;
import com.investory.tendency.presentation.dto.response.AnalysisRunAcceptedResponse;
import com.investory.tendency.presentation.dto.response.AnalysisRunDetailResponse;
import com.investory.tendency.presentation.dto.response.AnalysisRunListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/tendency")
public class TendencyController {

    private final AnalysisRunService analysisRunService;

    public TendencyController(AnalysisRunService analysisRunService) {
        this.analysisRunService = analysisRunService;
    }

    // 성향 분석 실행 — #207. 6개 항목 계산은 요청 스레드를 9~13초씩 붙잡던 원인이라 백그라운드로
    // 옮겼다. REQUESTED 상태의 실행만 즉시 만들고 202로 응답하며, 실제 진행 상태·결과는
    // GET /tendency/analyses/{analysisRunId}를 폴링해 확인한다.
    @PostMapping("/analyses")
    public ResponseEntity<AnalysisRunAcceptedResponse> runAnalysis(@AuthenticationPrincipal Long userId) {
        AnalysisRunAcceptedResult result = analysisRunService.runAnalysis(new RunAnalysisCommand(userId));
        return ResponseEntity.accepted()
                .location(URI.create("/tendency/analyses/" + result.analysisRunId()))
                .body(AnalysisRunAcceptedResponse.from(result));
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
