package com.investory.auth.domain.model;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.constant.UserStatusType;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;


// Service 에서 사용하는 User Object
@Getter
@Builder
public class User {
    private Long userId;
    private OAuthProviderType oauthProvider;
    private String oauthSubId;
    private String email;
    private String nickname;
    private UserStatusType userStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime withdrawnAt;

    public static User create(OAuthProviderType oauthProvider, OAuthUserInfo oAuthUserInfo) {
        return User.builder()
                .oauthProvider(oauthProvider)
                .oauthSubId(oAuthUserInfo.getOauthSubId())
                .email(oAuthUserInfo.getEmail())
                .nickname(oAuthUserInfo.getNickname())
                .userStatus(UserStatusType.ACTIVE)
                .build();
    }

    public static User of(Long userId, OAuthProviderType oauthProvider, String oauthSubId, String email,
                          String nickname, UserStatusType userStatus,
                          LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime withdrawnAt) {
        return User.builder()
                .userId(userId)
                .oauthProvider(oauthProvider)
                .oauthSubId(oauthSubId)
                .email(email)
                .nickname(nickname)
                .userStatus(userStatus)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .withdrawnAt(withdrawnAt)
                .build();
    }

    public boolean isWithdrawn() {
        return userStatus == UserStatusType.WITHDRAWN;
    }
}
