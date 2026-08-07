package com.investory.ledger.infra.repository_impls;

import com.investory.ledger.domain.model.HoldingFixture;
import com.investory.ledger.infra.entities.HoldingSnapshotRow;
import com.investory.ledger.infra.exception.LedgerInfraException;
import com.investory.ledger.infra.mappers.HoldingSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HoldingSnapshotRepositoryImplTest {

    @Test
    void findLatestByAccountIds_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        HoldingSnapshotRepositoryImpl repository = new HoldingSnapshotRepositoryImpl(new FailingHoldingSnapshotMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.findLatestByAccountIds(List.of(1L), null));

        assertSame(cause, exception.getCause());
    }

    @Test
    void upsert_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        HoldingSnapshotRepositoryImpl repository = new HoldingSnapshotRepositoryImpl(new FailingHoldingSnapshotMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.upsert(HoldingFixture.holding(1L, 101L, BigDecimal.TEN,
                        BigDecimal.valueOf(70000), BigDecimal.valueOf(75000), LocalDate.of(2026, 7, 29))));

        assertSame(cause, exception.getCause());
    }

    private static class FailingHoldingSnapshotMapper implements HoldingSnapshotMapper {
        private final RuntimeException toThrow;

        private FailingHoldingSnapshotMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public List<HoldingSnapshotRow> findLatestByAccountIds(List<Long> accountIds, Long securityId) {
            throw toThrow;
        }

        @Override
        public void upsert(HoldingSnapshotRow row) {
            throw toThrow;
        }
    }
}
