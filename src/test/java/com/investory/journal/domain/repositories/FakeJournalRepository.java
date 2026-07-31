package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.Journal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeJournalRepository implements JournalRepository {

    private final List<Journal> journals = new ArrayList<>();
    private long nextId = 1L;

    public void add(Journal... journals) {
        this.journals.addAll(List.of(journals));
    }

    @Override
    public List<Journal> findByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return journals.stream()
                .filter(journal -> !journal.getJournalDate().isBefore(startDate)
                        && !journal.getJournalDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Journal> findByUserAndDate(Long userId, LocalDate date) {
        return journals.stream()
                .filter(journal -> journal.getJournalDate().isEqual(date))
                .findFirst();
    }

    @Override
    public Optional<Journal> findById(Long journalId) {
        return journals.stream()
                .filter(journal -> journal.getJournalId().equals(journalId))
                .findFirst();
    }

    @Override
    public Journal save(Journal journal) {
        Journal saved = Journal.of(
                nextId++,
                journal.getUserId(),
                journal.getJournalDate(),
                journal.getMarketThought(),
                journal.getMarketMood(),
                journal.getTradeNoteCount(),
                journal.getCreatedAt(),
                journal.getUpdatedAt(),
                journal.getEditableUntilAt()
        );
        journals.add(saved);
        return saved;
    }
}
