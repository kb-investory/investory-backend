package com.investory.auth.domain.repositories;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();
    private long nextId = 1L;

    public void add(User user) {
        users.add(user);
    }

    @Override
    public Optional<User> findByOauthProviderAndOauthSubId(OAuthProviderType oauthProvider, String oauthSubId) {
        return users.stream()
                .filter(u -> u.getOauthProvider() == oauthProvider && u.getOauthSubId().equals(oauthSubId))
                .findFirst();
    }

    @Override
    public Optional<User> findByUserId(Long userId) {
        return users.stream().filter(u -> u.getUserId().equals(userId)).findFirst();
    }

    @Override
    public User save(User user) {
        if (user.getUserId() == null) {
            User saved = User.of(nextId++, user.getOauthProvider(), user.getOauthSubId(), user.getEmail(),
                    user.getNickname(), user.getUserStatus(), user.getCreatedAt(), user.getUpdatedAt(), user.getWithdrawnAt());
            users.add(saved);
            return saved;
        }
        users.removeIf(u -> u.getUserId().equals(user.getUserId()));
        users.add(user);
        return user;
    }
}
