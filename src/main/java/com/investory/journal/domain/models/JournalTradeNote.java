package com.investory.journal.domain.models;

import com.investory.journal.domain.constant.RationaleLabelType;
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
    private final RationaleLabelType rationaleLabelType;
    private final Instant createdAt;
    private final Instant updatedAt;

    private JournalTradeNote(Long journalTradeNoteId, Long journalId, Long tradeId, String rationaleText,
                              RationaleLabelType rationaleLabelType, Instant createdAt, Instant updatedAt) {
        requireNonNull(journalId);
        requireNonNull(tradeId);
        requireNonNull(rationaleText);
        requireNonNull(rationaleLabelType);
        requireNonNull(createdAt);
        requireNonNull(updatedAt);

        this.journalTradeNoteId = journalTradeNoteId;
        this.journalId = journalId;
        this.tradeId = tradeId;
        this.rationaleText = rationaleText;
        this.rationaleLabelType = rationaleLabelType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new JournalException(JournalErrorCode.INVALID_JOURNAL_DATA);
        }
    }

    // 신규 저장: journalTradeNoteId는 아직 없고(DB가 생성), createdAt/updatedAt은 저장 시점.
    public static JournalTradeNote create(Long journalId, Long tradeId, String rationaleText,
                                           RationaleLabelType rationaleLabelType) {
        Instant now = Instant.now();
        return new JournalTradeNote(null, journalId, tradeId, rationaleText, rationaleLabelType, now, now);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static JournalTradeNote of(Long journalTradeNoteId, Long journalId, Long tradeId, String rationaleText,
                                       RationaleLabelType rationaleLabelType, Instant createdAt, Instant updatedAt) {
        return new JournalTradeNote(journalTradeNoteId, journalId, tradeId, rationaleText, rationaleLabelType, createdAt, updatedAt);
    }
}
