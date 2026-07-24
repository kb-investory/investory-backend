package com.kbinvestory.backend.account.domain.repositories;

import com.kbinvestory.backend.account.domain.model.AccountConnection;

import java.util.Optional;

public interface AccountConnectionRepository {
    Optional<AccountConnection> findByUserIdAndProviderId(Long userId, Long providerId);
    AccountConnection save(AccountConnection accountConnection);
}