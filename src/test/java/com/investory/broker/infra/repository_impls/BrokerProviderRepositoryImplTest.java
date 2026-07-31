package com.investory.broker.infra.repository_impls;

import com.investory.broker.infra.entities.BrokerProviderRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.BrokerProviderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrokerProviderRepositoryImplTest {

    @Test
    void DB_예외는_BrokerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        BrokerProviderRepositoryImpl repository = new BrokerProviderRepositoryImpl(new FailingBrokerProviderMapper(cause));

        BrokerInfraException exception = assertThrows(BrokerInfraException.class, repository::findAllActive);

        assertSame(cause, exception.getCause());
    }

    private static class FailingBrokerProviderMapper implements BrokerProviderMapper {
        private final RuntimeException toThrow;

        private FailingBrokerProviderMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public List<BrokerProviderRow> findAllActive() {
            throw toThrow;
        }
    }
}