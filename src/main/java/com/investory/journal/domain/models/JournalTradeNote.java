package com.investory.journal.domain.models;

import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import lombok.Getter;

import java.time.Instant;

@Getter
public class JournalTradeNote {

    private final Long journalTradeNoteId;
    private final Long journalId;
    private final Long tradeId;
    private final String rationaleText;
    private final Instant createdAt;
    private final Instant updatedAt;

    private JournalTradeNote(Long journalTradeNoteId, Long journalId, Long tradeId,
                              String rationaleText, Instant createdAt, Instant updatedAt) {
        requireNonNull(journalTradeNoteId);
        requireNonNull(journalId);
        requireNonNull(tradeId);
        requireNonNull(rationaleText);
        requireNonNull(createdAt);
        requireNonNull(updatedAt);

        this.journalTradeNoteId = journalTradeNoteId;
        this.journalId = journalId;
        this.tradeId = tradeId;
        this.rationaleText = rationaleText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new JournalException(JournalErrorCode.INVALID_JOURNAL_DATA);
        }
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static JournalTradeNote of(Long journalTradeNoteId, Long journalId, Long tradeId,
                                       String rationaleText, Instant createdAt, Instant updatedAt) {
        return new JournalTradeNote(journalTradeNoteId, journalId, tradeId, rationaleText, createdAt, updatedAt);
    }
}
