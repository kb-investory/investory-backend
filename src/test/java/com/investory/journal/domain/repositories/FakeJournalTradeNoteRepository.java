package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.JournalTradeNote;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FakeJournalTradeNoteRepository implements JournalTradeNoteRepository {

    private final List<JournalTradeNote> notes = new ArrayList<>();
    private Map<String, Long> rationaleLabelCounts = Map.of();

    public void add(JournalTradeNote... notes) {
        this.notes.addAll(List.of(notes));
    }

    public void setRationaleLabelCounts(Map<String, Long> rationaleLabelCounts) {
        this.rationaleLabelCounts = rationaleLabelCounts;
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

    @Override
    public List<JournalTradeNote> findByJournalId(Long journalId) {
        return notes.stream()
                .filter(note -> note.getJournalId().equals(journalId))
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(List<JournalTradeNote> notes) {
        // upsert — 같은 tradeId가 이미 있으면 교체, 없으면 추가.
        for (JournalTradeNote note : notes) {
            this.notes.removeIf(existing -> existing.getTradeId().equals(note.getTradeId()));
            this.notes.add(note);
        }
    }

    @Override
    public void deleteByTradeIds(List<Long> tradeIds) {
        this.notes.removeIf(note -> tradeIds.contains(note.getTradeId()));
    }

    @Override
    public Map<String, Long> countRationaleLabelsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return rationaleLabelCounts;
    }

    public List<JournalTradeNote> getSaved() {
        return List.copyOf(notes);
    }
}
