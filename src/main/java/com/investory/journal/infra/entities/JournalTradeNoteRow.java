package com.investory.journal.infra.entities;

import com.investory.journal.domain.models.JournalTradeNote;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class JournalTradeNoteRow {
    private Long journalTradeNoteId;
    private Long journalId;
    private Long tradeId;
    private String rationaleText;
    private Instant createdAt;
    private Instant updatedAt;

    public JournalTradeNote toDomain() {
        return JournalTradeNote.of(journalTradeNoteId, journalId, tradeId, rationaleText, createdAt, updatedAt);
    }
}
