package com.investory.auth.presentation.dto.response;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.constant.UserStatusType;
import com.investory.auth.domain.services.dto.result.UserResult;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        OAuthProviderType oauthProvider,
        String email,
        String nickname,
        UserStatusType userStatus,
        LocalDateTime createdAt
) {
    public static UserResponse from(UserResult result) {
        return new UserResponse(
                result.userId(), result.oauthProvider(), result.email(),
                result.nickname(), result.userStatus(), result.createdAt()
        );
    }
}
