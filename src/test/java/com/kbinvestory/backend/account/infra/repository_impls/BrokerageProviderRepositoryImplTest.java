package com.kbinvestory.backend.account.infra.repository_impls;

import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;
import com.kbinvestory.backend.account.infra.entities.BrokerageProviderRow;
import com.kbinvestory.backend.account.infra.exception.AccountInfraErrorCode;
import com.kbinvestory.backend.account.infra.exception.AccountInfraException;
import com.kbinvestory.backend.account.infra.mappers.BrokerageProviderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrokerageProviderRepositoryImplTest {

    @Test
    void DB_예외는_AccountInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        BrokerageProviderRepositoryImpl repository = new BrokerageProviderRepositoryImpl(new FailingBrokerageProviderMapper(cause));

        AccountInfraException exception = assertThrows(AccountInfraException.class,
                () -> repository.search(new GetBrokersQuery(null)));

        assertEquals(AccountInfraErrorCode.BROKERAGE_PROVIDER_QUERY_FAILED, exception.getErrorCode());
        assertSame(cause, exception.getCause());
    }

    private static class FailingBrokerageProviderMapper implements BrokerageProviderMapper {
        private final RuntimeException toThrow;

        private FailingBrokerageProviderMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public List<BrokerageProviderRow> search(GetBrokersQuery query) {
            throw toThrow;
        }

        @Override
        public BrokerageProviderRow findById(Long providerId) {
            throw toThrow;
        }
    }
}
