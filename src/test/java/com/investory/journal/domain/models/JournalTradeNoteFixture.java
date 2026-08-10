package com.investory.journal.domain.models;

import com.investory.journal.domain.constant.RationaleLabelType;

import java.time.Instant;

public class JournalTradeNoteFixture {

    public static JournalTradeNote note(Long tradeId, String rationaleText, Instant createdAt) {
        return JournalTradeNote.of(701L, 305L, tradeId, rationaleText, RationaleLabelType.UNCLASSIFIED, createdAt, createdAt);
    }
}
