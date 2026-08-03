package com.investory.auth.domain.services.dto.result;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.constant.UserStatusType;
import com.investory.auth.domain.model.User;

import java.time.LocalDateTime;

public record UserResult(
        Long userId,
        OAuthProviderType oauthProvider,
        String oauthSubId,
        String email,
        String nickname,
        UserStatusType userStatus,
        LocalDateTime createdAt
) {
    public static UserResult from(User user) {
        return new UserResult(
                user.getUserId(),
                user.getOauthProvider(),
                user.getOauthSubId(),
                user.getEmail(),
                user.getNickname(),
                user.getUserStatus(),
                user.getCreatedAt()
        );
    }
}
