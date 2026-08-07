package com.investory.tendency.presentation.controller;

import com.investory.tendency.domain.services.LossResponseAnalysisService;
import com.investory.tendency.domain.services.dto.query.AnalyzeLossResponseQuery;
import com.investory.tendency.domain.services.dto.result.LossResponseAnalysisResult;
import com.investory.tendency.presentation.dto.response.LossResponseAnalysisResponse;
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

    public TendencyController(LossResponseAnalysisService lossResponseAnalysisService) {
        this.lossResponseAnalysisService = lossResponseAnalysisService;
    }

    @GetMapping("/loss-response")
    public LossResponseAnalysisResponse analyzeLossResponse(@RequestParam Long securityId) {
        LossResponseAnalysisResult result = lossResponseAnalysisService.analyze(
                new AnalyzeLossResponseQuery(TEMP_USER_ID, securityId));
        return LossResponseAnalysisResponse.from(result);
    }
}
