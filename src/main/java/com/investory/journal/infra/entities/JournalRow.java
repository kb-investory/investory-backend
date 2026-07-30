package com.investory.journal.infra.entities;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.models.Journal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class JournalRow {
    private Long journalId;
    private LocalDate journalDate;
    private MarketMood marketMood;
    private int tradeNoteCount;
    private Instant createdAt;
    private Instant editableUntilAt;

    public Journal toDomain() {
        return Journal.of(journalId, journalDate, marketMood, tradeNoteCount, createdAt, editableUntilAt);
    }
}
