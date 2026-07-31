package com.investory.journal.domain.models;

import com.investory.journal.domain.constant.MarketMood;

import java.time.Instant;
import java.time.LocalDate;

public class JournalFixture {

    public static Journal journal(LocalDate journalDate, Instant createdAt, Instant editableUntilAt) {
        return journal(1L, 100L, journalDate, createdAt, editableUntilAt);
    }

    public static Journal journal(Long journalId, Long userId, LocalDate journalDate, Instant createdAt, Instant editableUntilAt) {
        return Journal.of(journalId, userId, journalDate, "시장에 대한 생각", MarketMood.CALM, 2, createdAt, createdAt, editableUntilAt);
    }
}
