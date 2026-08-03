package com.investory.broker.infra.repository_impls;

import com.investory.broker.infra.entities.BrokerConnectionRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.BrokerConnectionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrokerConnectionRepositoryImplTest {

    @Test
    void DB_예외는_BrokerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        BrokerConnectionRepositoryImpl repository = new BrokerConnectionRepositoryImpl(new FailingBrokerConnectionMapper(cause));

        BrokerInfraException exception = assertThrows(BrokerInfraException.class, () -> repository.findAllByUserId(1L));

        assertSame(cause, exception.getCause());
    }

    private static class FailingBrokerConnectionMapper implements BrokerConnectionMapper {
        private final RuntimeException toThrow;

        private FailingBrokerConnectionMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public List<BrokerConnectionRow> findAllByUserId(Long userId) {
            throw toThrow;
        }

        @Override
        public List<BrokerConnectionRow> findActiveByUserIdAndBrokerId(Long userId, Long brokerId) {
            throw toThrow;
        }

        @Override
        public List<BrokerConnectionRow> findByIdAndUserId(Long connectionId, Long userId) {
            throw toThrow;
        }

        @Override
        public void insert(BrokerConnectionRow row) {
            throw toThrow;
        }

        @Override
        public void updateLastSyncedAt(Long connectionId, Instant lastSyncedAt) {
            throw toThrow;
        }
    }
}
