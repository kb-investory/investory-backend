package com.investory.journal.infra.repository_impls;

import com.investory.journal.domain.constant.RationaleLabelType;
import com.investory.journal.domain.models.JournalTradeNote;
import com.investory.journal.infra.entities.JournalTradeNoteRow;
import com.investory.journal.infra.entities.RationaleLabelCountRow;
import com.investory.journal.infra.exception.JournalInfraException;
import com.investory.journal.infra.mappers.JournalTradeNoteMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JournalTradeNoteRepositoryImplTest {

    @Test
    void findByTradeIds_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(new FailingJournalTradeNoteMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.findByTradeIds(List.of(1L)));

        assertSame(cause, exception.getCause());
    }

    @Test
    void tradeIds가_비어있으면_매퍼를_호출하지_않고_빈_리스트를_반환한다() {
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(new NeverCalledJournalTradeNoteMapper());

        List<?> result = repository.findByTradeIds(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByJournalId_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(new FailingJournalTradeNoteMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.findByJournalId(305L));

        assertSame(cause, exception.getCause());
    }

    @Test
    void saveAll_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(new FailingJournalTradeNoteMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.saveAll(List.of(JournalTradeNote.create(1L, 501L, "판단 근거", RationaleLabelType.UNCLASSIFIED))));

        assertSame(cause, exception.getCause());
    }

    @Test
    void notes가_비어있으면_매퍼를_호출하지_않는다() {
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(new NeverCalledJournalTradeNoteMapper());

        repository.saveAll(List.of());
        // NeverCalledJournalTradeNoteMapper가 호출되면 fail()이 터지므로, 여기까지 도달하면 통과
    }

    @Test
    void deleteByTradeIds_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(new FailingJournalTradeNoteMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.deleteByTradeIds(List.of(501L)));

        assertSame(cause, exception.getCause());
    }

    @Test
    void tradeIds가_비어있으면_삭제_매퍼를_호출하지_않는다() {
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(new NeverCalledJournalTradeNoteMapper());

        repository.deleteByTradeIds(List.of());
        // NeverCalledJournalTradeNoteMapper가 호출되면 fail()이 터지므로, 여기까지 도달하면 통과
    }

    @Test
    void countRationaleLabelsByUserAndDateRange_DB_예외는_JournalInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(new FailingJournalTradeNoteMapper(cause));

        JournalInfraException exception = assertThrows(JournalInfraException.class,
                () -> repository.countRationaleLabelsByUserAndDateRange(1L, LocalDate.now().minusDays(90), LocalDate.now()));

        assertSame(cause, exception.getCause());
    }

    @Test
    void countRationaleLabelsByUserAndDateRange_매퍼_행을_원본_문자열_키의_맵으로_변환한다() {
        JournalTradeNoteRepositoryImpl repository = new JournalTradeNoteRepositoryImpl(
                new StubRationaleLabelStatsJournalTradeNoteMapper(List.of(
                        row("unclassified", 3),
                        row("FUNDAMENTAL_ANALYSIS", 2)
                )));

        Map<String, Long> counts = repository.countRationaleLabelsByUserAndDateRange(1L, LocalDate.now().minusDays(90), LocalDate.now());

        assertEquals(3L, counts.get("unclassified"));
        assertEquals(2L, counts.get("FUNDAMENTAL_ANALYSIS"));
    }

    private static RationaleLabelCountRow row(String label, long count) {
        RationaleLabelCountRow row = new RationaleLabelCountRow();
        row.setRationaleLabel(label);
        row.setCount(count);
        return row;
    }

    private static class FailingJournalTradeNoteMapper implements JournalTradeNoteMapper {
        private final RuntimeException toThrow;

        private FailingJournalTradeNoteMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public List<JournalTradeNoteRow> findByTradeIds(List<Long> tradeIds) {
            throw toThrow;
        }

        @Override
        public List<JournalTradeNoteRow> findByJournalId(Long journalId) {
            throw toThrow;
        }

        @Override
        public void insertAll(List<JournalTradeNoteRow> notes) {
            throw toThrow;
        }

        @Override
        public void deleteByTradeIds(List<Long> tradeIds) {
            throw toThrow;
        }

        @Override
        public List<RationaleLabelCountRow> countRationaleLabelsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
            throw toThrow;
        }
    }

    private static class StubRationaleLabelStatsJournalTradeNoteMapper implements JournalTradeNoteMapper {
        private final List<RationaleLabelCountRow> rows;

        private StubRationaleLabelStatsJournalTradeNoteMapper(List<RationaleLabelCountRow> rows) {
            this.rows = rows;
        }

        @Override
        public List<JournalTradeNoteRow> findByTradeIds(List<Long> tradeIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<JournalTradeNoteRow> findByJournalId(Long journalId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void insertAll(List<JournalTradeNoteRow> notes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteByTradeIds(List<Long> tradeIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RationaleLabelCountRow> countRationaleLabelsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
            return rows;
        }
    }

    private static class NeverCalledJournalTradeNoteMapper implements JournalTradeNoteMapper {
        @Override
        public List<JournalTradeNoteRow> findByTradeIds(List<Long> tradeIds) {
            fail("빈 tradeIds에 대해서는 매퍼가 호출되면 안 된다");
            return List.of();
        }

        @Override
        public List<JournalTradeNoteRow> findByJournalId(Long journalId) {
            fail("이 테스트에서는 호출되면 안 된다");
            return List.of();
        }

        @Override
        public void insertAll(List<JournalTradeNoteRow> notes) {
            fail("빈 notes에 대해서는 매퍼가 호출되면 안 된다");
        }

        @Override
        public void deleteByTradeIds(List<Long> tradeIds) {
            fail("빈 tradeIds에 대해서는 매퍼가 호출되면 안 된다");
        }

        @Override
        public List<RationaleLabelCountRow> countRationaleLabelsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
            fail("이 테스트에서는 호출되면 안 된다");
            return List.of();
        }
    }
}
