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

    @Test
    void upsertByCode_DB_예외는_BrokerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        BrokerProviderRepositoryImpl repository = new BrokerProviderRepositoryImpl(new FailingBrokerProviderMapper(cause));

        BrokerInfraException exception = assertThrows(BrokerInfraException.class,
                () -> repository.upsertByCode("S9990001A", "미래에셋증권(모의)"));

        assertSame(cause, exception.getCause());
    }

    @Test
    void deactivateExcept_DB_예외는_BrokerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        BrokerProviderRepositoryImpl repository = new BrokerProviderRepositoryImpl(new FailingBrokerProviderMapper(cause));

        BrokerInfraException exception = assertThrows(BrokerInfraException.class,
                () -> repository.deactivateExcept(List.of("S9990001A")));

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

        @Override
        public List<BrokerProviderRow> findById(Long brokerId) {
            throw toThrow;
        }

        @Override
        public List<BrokerProviderRow> findByCode(String brokerCode) {
            throw toThrow;
        }

        @Override
        public void insert(BrokerProviderRow row) {
            throw toThrow;
        }

        @Override
        public void updateByCode(String brokerCode, String brokerName) {
            throw toThrow;
        }

        @Override
        public void deactivateExcept(List<String> brokerCodes) {
            throw toThrow;
        }
    }
}