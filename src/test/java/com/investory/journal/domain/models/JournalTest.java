package com.investory.journal.domain.models;

import com.investory.journal.domain.constant.MarketMood;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalTest {

    @Test
    void 작성일과_같은_날_작성했으면_isBackfilled는_false다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        Instant createdAt = journalDate.atTime(23, 59).toInstant(ZoneOffset.UTC);
        Journal journal = JournalFixture.journal(journalDate, createdAt, Instant.now().plusSeconds(3600));

        assertFalse(journal.isBackfilled());
    }

    @Test
    void 대상_날짜보다_늦게_작성했으면_isBackfilled는_true다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        Instant createdAt = journalDate.plusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC);
        Journal journal = JournalFixture.journal(journalDate, createdAt, Instant.now().plusSeconds(3600));

        assertTrue(journal.isBackfilled());
    }

    @Test
    void 현재_시각이_editableUntilAt_이전이면_isEditable은_true다() {
        Journal journal = JournalFixture.journal(LocalDate.of(2026, 7, 10), Instant.now(), Instant.now().plusSeconds(3600));

        assertTrue(journal.isEditable(Instant.now()));
    }

    @Test
    void 현재_시각이_editableUntilAt_이후면_isEditable은_false다() {
        Journal journal = JournalFixture.journal(LocalDate.of(2026, 7, 10), Instant.now(), Instant.now().minusSeconds(3600));

        assertFalse(journal.isEditable(Instant.now()));
    }

    @Test
    void update하면_marketThought와_marketMood와_updatedAt만_바뀌고_나머지는_유지된다() throws InterruptedException {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        Instant createdAt = journalDate.atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant editableUntilAt = Instant.now().plusSeconds(3600);
        Journal journal = JournalFixture.journal(1L, 100L, journalDate, createdAt, editableUntilAt);

        Thread.sleep(2); // updatedAt이 실제로 바뀌었는지 확인하기 위해 시간 차를 둔다
        Journal updated = journal.update("바뀐 생각", MarketMood.CONFIDENT);

        assertEquals("바뀐 생각", updated.getMarketThought());
        assertEquals(MarketMood.CONFIDENT, updated.getMarketMood());
        assertTrue(updated.getUpdatedAt().isAfter(journal.getUpdatedAt()));
        assertEquals(journal.getJournalId(), updated.getJournalId());
        assertEquals(journal.getUserId(), updated.getUserId());
        assertEquals(journal.getJournalDate(), updated.getJournalDate());
        assertEquals(journal.getCreatedAt(), updated.getCreatedAt());
        assertEquals(journal.getEditableUntilAt(), updated.getEditableUntilAt());
    }
}
