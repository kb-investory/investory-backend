package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.JournalTradeNote;

import java.util.List;

public interface JournalTradeNoteRepository {
    List<JournalTradeNote> findByTradeIds(List<Long> tradeIds);
}
