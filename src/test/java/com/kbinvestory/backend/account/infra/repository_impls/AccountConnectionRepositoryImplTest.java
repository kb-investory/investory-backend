package com.kbinvestory.backend.account.infra.repository_impls;

import com.kbinvestory.backend.account.domain.model.AccountConnection;
import com.kbinvestory.backend.account.infra.entities.AccountConnectionRow;
import com.kbinvestory.backend.account.infra.exception.AccountInfraErrorCode;
import com.kbinvestory.backend.account.infra.exception.AccountInfraException;
import com.kbinvestory.backend.account.infra.mappers.AccountConnectionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountConnectionRepositoryImplTest {

    @Test
    void 조회_중_DB_예외는_AccountInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        AccountConnectionRepositoryImpl repository = new AccountConnectionRepositoryImpl(new FailingAccountConnectionMapper(cause));

        AccountInfraException exception = assertThrows(AccountInfraException.class,
                () -> repository.findByUserIdAndProviderId(1L, 1L));

        assertEquals(AccountInfraErrorCode.ACCOUNT_CONNECTION_QUERY_FAILED, exception.getErrorCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    void 저장_중_DB_예외는_AccountInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        AccountConnectionRepositoryImpl repository = new AccountConnectionRepositoryImpl(new FailingAccountConnectionMapper(cause));

        AccountInfraException exception = assertThrows(AccountInfraException.class,
                () -> repository.save(AccountConnection.create(1L, 1L, "CONNECTED_ID")));

        assertEquals(AccountInfraErrorCode.ACCOUNT_CONNECTION_SAVE_FAILED, exception.getErrorCode());
        assertSame(cause, exception.getCause());
    }

    private static class FailingAccountConnectionMapper implements AccountConnectionMapper {
        private final RuntimeException toThrow;

        private FailingAccountConnectionMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public AccountConnectionRow findByUserIdAndProviderId(Long userId, Long providerId) {
            throw toThrow;
        }

        @Override
        public void upsert(AccountConnectionRow row) {
            throw toThrow;
        }
    }
}
