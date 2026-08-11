package com.investory.principle.presentation.controller;

import com.investory.principle.domain.services.PrincipleService;
import com.investory.principle.domain.services.dto.query.GetActivePrincipleSetQuery;
import com.investory.principle.domain.services.dto.query.GetRecommendationsQuery;
import com.investory.principle.domain.services.dto.result.PrincipleSetResult;
import com.investory.principle.domain.services.dto.result.RecommendationListResult;
import com.investory.principle.domain.services.dto.result.SavePrincipleSetResult;
import com.investory.principle.presentation.dto.request.SavePrincipleSetRequest;
import com.investory.principle.presentation.dto.response.PrincipleRecommendationListResponse;
import com.investory.principle.presentation.dto.response.PrincipleSetResponse;
import com.investory.principle.presentation.dto.response.SavePrincipleSetResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/principle")
public class PrincipleController {

    // TODO: JWT 인증 도입 후 Principal.userId로 교체 (auth 도메인 미구현으로 임시 고정값 사용)
    private static final Long TEMP_USER_ID = 1L;

    private final PrincipleService principleService;

    public PrincipleController(PrincipleService principleService) {
        this.principleService = principleService;
    }

    @GetMapping
    public PrincipleSetResponse getPrincipleSet() {
        PrincipleSetResult result = principleService.getActivePrincipleSet(new GetActivePrincipleSetQuery(TEMP_USER_ID));
        return PrincipleSetResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavePrincipleSetResponse savePrincipleSet(@RequestBody SavePrincipleSetRequest request) {
        SavePrincipleSetResult result = principleService.savePrincipleSet(request.toCommand(TEMP_USER_ID));
        return SavePrincipleSetResponse.from(result);
    }

    @GetMapping("/recommendations")
    public PrincipleRecommendationListResponse getRecommendations() {
        RecommendationListResult result = principleService.getRecommendations(new GetRecommendationsQuery(TEMP_USER_ID));
        return PrincipleRecommendationListResponse.from(result);
    }
}
