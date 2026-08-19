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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/principle")
public class PrincipleController {

    private final PrincipleService principleService;

    public PrincipleController(PrincipleService principleService) {
        this.principleService = principleService;
    }

    @GetMapping
    public PrincipleSetResponse getPrincipleSet(@AuthenticationPrincipal Long userId) {
        PrincipleSetResult result = principleService.getActivePrincipleSet(new GetActivePrincipleSetQuery(userId));
        return PrincipleSetResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavePrincipleSetResponse savePrincipleSet(
            @AuthenticationPrincipal Long userId, @RequestBody SavePrincipleSetRequest request) {
        SavePrincipleSetResult result = principleService.savePrincipleSet(request.toCommand(userId));
        return SavePrincipleSetResponse.from(result);
    }

    @GetMapping("/recommendations")
    public PrincipleRecommendationListResponse getRecommendations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long analysisRunId) {
        RecommendationListResult result = principleService.getRecommendations(new GetRecommendationsQuery(userId, analysisRunId));
        return PrincipleRecommendationListResponse.from(result);
    }
}
