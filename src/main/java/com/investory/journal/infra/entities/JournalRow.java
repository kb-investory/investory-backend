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
    private Long userId;
    private LocalDate journalDate;
    private String marketThought;
    private MarketMood marketMood;
    private int tradeNoteCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant editableUntilAt;

    public Journal toDomain() {
        return Journal.of(journalId, userId, journalDate, marketThought, marketMood, tradeNoteCount, createdAt, updatedAt, editableUntilAt);
    }

    public static JournalRow from(Journal journal) {
        JournalRow row = new JournalRow();
        row.journalId = journal.getJournalId();
        row.userId = journal.getUserId();
        row.journalDate = journal.getJournalDate();
        row.marketThought = journal.getMarketThought();
        row.marketMood = journal.getMarketMood();
        row.tradeNoteCount = journal.getTradeNoteCount();
        row.createdAt = journal.getCreatedAt();
        row.updatedAt = journal.getUpdatedAt();
        row.editableUntilAt = journal.getEditableUntilAt();
        return row;
    }
}
