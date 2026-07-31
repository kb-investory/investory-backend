package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.JournalTradeNote;

import java.util.List;

public interface JournalTradeNoteRepository {
    List<JournalTradeNote> findByTradeIds(List<Long> tradeIds);
    List<JournalTradeNote> findByJournalId(Long journalId);
    void saveAll(List<JournalTradeNote> notes); // upsert — 있으면 갱신, 없으면 생성
    void deleteByTradeIds(List<Long> tradeIds);
}
