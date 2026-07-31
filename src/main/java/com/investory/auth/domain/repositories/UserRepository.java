package com.investory.auth.domain.repositories;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByOauthProviderAndOauthSubId(OAuthProviderType oauthProvider, String oauthSubId);
    Optional<User> findByUserId(Long userId);
    User save(User user);
}
