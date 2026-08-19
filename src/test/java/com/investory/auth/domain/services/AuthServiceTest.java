package com.investory.auth.domain.services;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.constant.UserStatusType;
import com.investory.auth.domain.exception.AuthErrorCode;
import com.investory.auth.domain.exception.AuthException;
import com.investory.auth.domain.model.User;
import com.investory.auth.domain.ports.FakeBrokerConnectionCleanupPort;
import com.investory.auth.domain.ports.FakeJournalCleanupPort;
import com.investory.auth.domain.ports.FakePrincipleCleanupPort;
import com.investory.auth.domain.ports.FakeTokenProvider;
import com.investory.auth.domain.repositories.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private static final Long USER_ID = 100L;

    private FakeUserRepository userRepository;
    private FakeBrokerConnectionCleanupPort brokerConnectionCleanupPort;
    private FakeJournalCleanupPort journalCleanupPort;
    private FakePrincipleCleanupPort principleCleanupPort;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        brokerConnectionCleanupPort = new FakeBrokerConnectionCleanupPort();
        journalCleanupPort = new FakeJournalCleanupPort();
        principleCleanupPort = new FakePrincipleCleanupPort();
        authService = new AuthService(List.of(), userRepository, new FakeTokenProvider(),
                brokerConnectionCleanupPort, journalCleanupPort, principleCleanupPort);

        userRepository.add(activeUser());
    }

    @Test
    void 탈퇴하면_다른_도메인_정리를_전부_호출하고_회원_상태가_WITHDRAWN으로_바뀐다() {
        authService.withdraw(USER_ID);

        assertEquals(List.of(USER_ID), brokerConnectionCleanupPort.calledForUserIds());
        assertEquals(List.of(USER_ID), journalCleanupPort.calledForUserIds());
        assertEquals(List.of(USER_ID), principleCleanupPort.calledForUserIds());

        User withdrawn = userRepository.findByUserId(USER_ID).orElseThrow();
        assertEquals(UserStatusType.WITHDRAWN, withdrawn.getUserStatus());
        assertTrue(withdrawn.isWithdrawn());
    }

    @Test
    void 이미_탈퇴한_회원을_다시_탈퇴시키면_다른_도메인_정리가_재실행되지_않는다() {
        authService.withdraw(USER_ID);
        authService.withdraw(USER_ID);

        assertEquals(1, brokerConnectionCleanupPort.calledForUserIds().size());
        assertEquals(1, journalCleanupPort.calledForUserIds().size());
        assertEquals(1, principleCleanupPort.calledForUserIds().size());
    }

    @Test
    void 존재하지_않는_회원을_탈퇴시키면_예외가_발생한다() {
        AuthException exception = assertThrows(AuthException.class, () -> authService.withdraw(999L));

        assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    private User activeUser() {
        return User.of(USER_ID, OAuthProviderType.KAKAO, "sub-1", "user@example.com", "닉네임",
                UserStatusType.ACTIVE, LocalDateTime.now(), LocalDateTime.now(), null);
    }
}
