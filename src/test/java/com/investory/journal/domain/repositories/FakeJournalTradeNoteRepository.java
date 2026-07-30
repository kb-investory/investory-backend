package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.JournalTradeNote;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeJournalTradeNoteRepository implements JournalTradeNoteRepository {

    private final List<JournalTradeNote> notes = new ArrayList<>();

    public void add(JournalTradeNote... notes) {
        this.notes.addAll(List.of(notes));
    }

    @Override
    public List<JournalTradeNote> findByTradeIds(List<Long> tradeIds) {
        if (tradeIds.isEmpty()) {
            return List.of();
        }
        return notes.stream()
                .filter(note -> tradeIds.contains(note.getTradeId()))
                .collect(Collectors.toList());
    }
}
