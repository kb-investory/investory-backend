package com.investory.auth.infra.repository_impls;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.model.User;
import com.investory.auth.domain.repositories.UserRepository;
import com.investory.auth.infra.entities.UserRow;
import com.investory.auth.infra.exception.AuthInfraErrorCode;
import com.investory.auth.infra.exception.AuthInfraException;
import com.investory.auth.infra.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    // provider + subId(소셜 고유 ID) 조합으로 회원을 조회한다. 로그인 시 "이미 가입된 회원인지" 판단하는 기준이 된다.
    @Override
    public Optional<User> findByOauthProviderAndOauthSubId(OAuthProviderType oauthProvider, String oauthSubId) {
        try {
            UserRow row = userMapper.findBySocialProviderAndSocialSubject(oauthProvider.name(), oauthSubId);
            return Optional.ofNullable(row).map(UserRow::toDomain);
        } catch (DataAccessException e) {
            throw new AuthInfraException(AuthInfraErrorCode.USER_QUERY_FAILED, e);
        }
    }

    // 서비스의 PK(userId)로 회원을 조회한다. JWT에 담긴 userId로 회원을 찾을 때 사용한다.
    @Override
    public Optional<User> findByUserId(Long userId) {
        try {
            UserRow row = userMapper.findByUserId(userId);
            return Optional.ofNullable(row).map(UserRow::toDomain);
        } catch (DataAccessException e) {
            throw new AuthInfraException(AuthInfraErrorCode.USER_QUERY_FAILED, e);
        }
    }

    // userId가 없으면 신규 회원이므로 insert, 있으면 기존 회원이므로 update한다.
    // 신규 insert는 DB의 (social_provider, social_subject) UNIQUE 제약과 함께 동작해
    @Override
    public User save(User user) {
        try {
            UserRow row = UserRow.from(user);
            if (row.getUserId() == null) {
                userMapper.insert(row);
            } else {
                userMapper.update(row);
            }
            return row.toDomain();
        } catch (DataAccessException e) {
            throw new AuthInfraException(AuthInfraErrorCode.USER_SAVE_FAILED, e);
        }
    }
}
