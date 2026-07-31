package com.investory.journal.infra.repository_impls;

import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.models.JournalFixture;
import com.investory.journal.infra.entities.JournalRow;
import com.investory.journal.infra.exception.JournalInfraException;
import com.investory.journal.infra.mappers.JournalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalRepositoryImplTest {

    @Test
    void findByUserAndDateRange_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalRepositoryImpl repository = new JournalRepositoryImpl(new FailingJournalMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.findByUserAndDateRange(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

        assertSame(cause, exception.getCause());
    }

    @Test
    void findByUserAndDate_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalRepositoryImpl repository = new JournalRepositoryImpl(new FailingJournalMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.findByUserAndDate(1L, LocalDate.of(2026, 7, 1)));

        assertSame(cause, exception.getCause());
    }

    @Test
    void save_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalRepositoryImpl repository = new JournalRepositoryImpl(new FailingJournalMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.save(sampleJournal()));

        assertSame(cause, exception.getCause());
    }

    @Test
    void save_DuplicateKeyException은_JOURNAL_ALREADY_EXISTS로_변환된다() {
        DuplicateKeyException cause = new DuplicateKeyException("unique constraint violated");
        JournalRepositoryImpl repository = new JournalRepositoryImpl(new FailingJournalMapper(cause));

        JournalException exception = assertThrows(JournalException.class,
                () -> repository.save(sampleJournal()));

        assertEquals(JournalErrorCode.JOURNAL_ALREADY_EXISTS, exception.getErrorCode());
    }

    private Journal sampleJournal() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        return JournalFixture.journal(journalDate, Instant.now(), Instant.now().plusSeconds(3600));
    }

    private static class FailingJournalMapper implements JournalMapper {
        private final RuntimeException toThrow;

        private FailingJournalMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public List<JournalRow> findByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
            throw toThrow;
        }

        @Override
        public List<JournalRow> findByUserAndDate(Long userId, LocalDate date) {
            throw toThrow;
        }

        @Override
        public void insert(JournalRow row) {
            throw toThrow;
        }
    }
}
