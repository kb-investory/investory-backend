package com.investory.auth.presentation.dto.response;

import com.investory.auth.domain.services.dto.result.ReissueResult;

public record ReissueResponse(String accessToken, String tokenType, long expiresIn) {
    public static ReissueResponse from(ReissueResult result) {
        return new ReissueResponse(result.accessToken(), "Bearer ", result.expiresIn());
    }
}
