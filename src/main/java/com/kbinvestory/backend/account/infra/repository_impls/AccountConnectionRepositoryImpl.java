package com.kbinvestory.backend.account.infra.repository_impls;

import com.kbinvestory.backend.account.domain.model.AccountConnection;
import com.kbinvestory.backend.account.domain.repositories.AccountConnectionRepository;
import com.kbinvestory.backend.account.infra.entities.AccountConnectionRow;
import com.kbinvestory.backend.account.infra.exception.AccountInfraErrorCode;
import com.kbinvestory.backend.account.infra.exception.AccountInfraException;
import com.kbinvestory.backend.account.infra.mappers.AccountConnectionMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountConnectionRepositoryImpl implements AccountConnectionRepository {

    private final AccountConnectionMapper accountConnectionMapper;

    public AccountConnectionRepositoryImpl(AccountConnectionMapper accountConnectionMapper) {
        this.accountConnectionMapper = accountConnectionMapper;
    }

    @Override
    public Optional<AccountConnection> findByUserIdAndProviderId(Long userId, Long providerId) {
        try {
            return Optional.ofNullable(accountConnectionMapper.findByUserIdAndProviderId(userId, providerId))
                    .map(AccountConnectionRow::toDomain);
        } catch (DataAccessException e) {
            throw new AccountInfraException(AccountInfraErrorCode.ACCOUNT_CONNECTION_QUERY_FAILED, e);
        }
    }

    @Override
    public AccountConnection save(AccountConnection accountConnection) {
        try {
            AccountConnectionRow row = AccountConnectionRow.from(accountConnection);
            accountConnectionMapper.insert(row);
            return row.toDomain();
        } catch (DataAccessException e) {
            throw new AccountInfraException(AccountInfraErrorCode.ACCOUNT_CONNECTION_SAVE_FAILED, e);
        }
    }
}
