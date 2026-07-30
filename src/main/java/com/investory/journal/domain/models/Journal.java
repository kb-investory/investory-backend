package com.investory.journal.domain.models;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
public class Journal {

    private final Long journalId;
    private final LocalDate journalDate;
    private final MarketMood marketMood;
    private final int tradeNoteCount;
    private final Instant createdAt;
    private final Instant editableUntilAt;

    private Journal(Long journalId, LocalDate journalDate, MarketMood marketMood,
                     int tradeNoteCount, Instant createdAt, Instant editableUntilAt) {
        requireNonNull(journalId);
        requireNonNull(journalDate);
        requireNonNull(createdAt);
        requireNonNull(editableUntilAt);

        this.journalId = journalId;
        this.journalDate = journalDate;
        this.marketMood = marketMood;
        this.tradeNoteCount = tradeNoteCount;
        this.createdAt = createdAt;
        this.editableUntilAt = editableUntilAt;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new JournalException(JournalErrorCode.INVALID_JOURNAL_DATA);
        }
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용). marketMood는 선택 입력이라 null 허용.
    public static Journal of(Long journalId, LocalDate journalDate, MarketMood marketMood,
                              int tradeNoteCount, Instant createdAt, Instant editableUntilAt) {
        return new Journal(journalId, journalDate, marketMood, tradeNoteCount, createdAt, editableUntilAt);
    }
}
