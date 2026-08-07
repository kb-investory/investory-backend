package com.investory.auth.infra.entities;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.constant.UserStatusType;
import com.investory.auth.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRow {
    private Long userId;
    private String socialProvider;
    private String socialSubject;
    private String nickname;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime withdrawnAt;

    public static UserRow from(User user) {
        UserRow row = new UserRow();
        row.userId = user.getUserId();
        row.socialProvider = user.getOauthProvider().name();
        row.socialSubject = user.getOauthSubId();
        row.nickname = user.getNickname();
        row.email = user.getEmail();
        row.status = user.getUserStatus().name();
        row.createdAt = user.getCreatedAt();
        row.updatedAt = user.getUpdatedAt();
        row.withdrawnAt = user.getWithdrawnAt();
        return row;
    }

    public User toDomain() {
        return User.of(
                userId,
                OAuthProviderType.valueOf(socialProvider),
                socialSubject,
                email,
                nickname,
                UserStatusType.valueOf(status),
                createdAt,
                updatedAt,
                withdrawnAt
        );
    }
}
