package com.investory.journal.domain.services;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.ports.FakeTradeLedgerPort;
import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.repositories.FakeJournalRepository;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalServiceTest {

    private static final Long USER_ID = 100L;

    private FakeJournalRepository journalRepository;
    private FakeTradeLedgerPort tradeLedgerPort;
    private JournalService journalService;

    @BeforeEach
    void setUp() {
        journalRepository = new FakeJournalRepository();
        tradeLedgerPort = new FakeTradeLedgerPort();
        journalService = new JournalService(journalRepository, tradeLedgerPort);
    }

    @Test
    void ledger의_tradeCount와_journal의_tradeNoteCount를_병합해서_반환한다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(20, 0), 3600));
        tradeLedgerPort.add(new TradeCountInfo(journalDate, 5));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertEquals(1, results.size());
        assertEquals(5, results.get(0).tradeCount());
        assertEquals(2, results.get(0).tradeNoteCount());
    }

    @Test
    void ledger에_해당_날짜_거래_기록이_없으면_tradeCount는_0이다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(20, 0), 3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertEquals(0, results.get(0).tradeCount());
    }

    @Test
    void 일지_대상_날짜와_같은_날_작성했으면_isBackfilled는_false다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(23, 59), 3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertFalse(results.get(0).isBackfilled());
    }

    @Test
    void 일지_대상_날짜보다_늦게_작성했으면_isBackfilled는_true다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.plusDays(3).atStartOfDay(), 3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertTrue(results.get(0).isBackfilled());
    }

    @Test
    void 현재_시각이_editableUntilAt_이전이면_isEditable은_true다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(10, 0), 3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertTrue(results.get(0).isEditable());
    }

    @Test
    void 현재_시각이_editableUntilAt_이후면_isEditable은_false다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(10, 0), -3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertFalse(results.get(0).isEditable());
    }

    @Test
    void startDate가_endDate보다_늦으면_예외를_던진다() {
        LocalDate startDate = LocalDate.of(2026, 7, 20);
        LocalDate endDate = LocalDate.of(2026, 7, 10);

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getEntries(new GetJournalEntriesQuery(USER_ID, startDate, endDate)));

        assertEquals(JournalErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }

    private List<JournalEntryResult> getEntries(LocalDate startDate, LocalDate endDate) {
        return journalService.getEntries(new GetJournalEntriesQuery(USER_ID, startDate, endDate));
    }

    // editableUntilAtOffsetSeconds: editableUntilAt = now + offset (음수면 이미 지난 시각)
    private Journal journal(LocalDate journalDate, LocalDateTime createdAtLocal, long editableUntilAtOffsetSeconds) {
        return Journal.of(
                1L,
                journalDate,
                MarketMood.CALM,
                2,
                createdAtLocal.toInstant(ZoneOffset.UTC),
                Instant.now().plusSeconds(editableUntilAtOffsetSeconds)
        );
    }
}
