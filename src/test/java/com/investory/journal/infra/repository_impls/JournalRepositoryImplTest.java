package com.investory.journal.infra.repository_impls;

import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.infra.entities.JournalRow;
import com.investory.journal.infra.exception.JournalInfraException;
import com.investory.journal.infra.mappers.JournalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalRepositoryImplTest {

    @Test
    void findByUserAndDateRange_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalRepositoryImpl repository = new JournalRepositoryImpl(new FailingJournalMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.findByUserAndDateRange(new GetJournalEntriesQuery(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))));

        assertSame(cause, exception.getCause());
    }

    @Test
    void findByUserAndDate_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalRepositoryImpl repository = new JournalRepositoryImpl(new FailingJournalMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.findByUserAndDate(new GetJournalDetailQuery(1L, LocalDate.of(2026, 7, 1))));

        assertSame(cause, exception.getCause());
    }

    private static class FailingJournalMapper implements JournalMapper {
        private final RuntimeException toThrow;

        private FailingJournalMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public List<JournalRow> findByUserAndDateRange(GetJournalEntriesQuery query) {
            throw toThrow;
        }

        @Override
        public List<JournalRow> findByUserAndDate(GetJournalDetailQuery query) {
            throw toThrow;
        }
    }
}
