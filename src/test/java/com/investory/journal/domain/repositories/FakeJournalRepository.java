package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeJournalRepository implements JournalRepository {

    private final List<Journal> journals = new ArrayList<>();

    public void add(Journal... journals) {
        this.journals.addAll(List.of(journals));
    }

    @Override
    public List<Journal> findByUserAndDateRange(GetJournalEntriesQuery query) {
        return journals.stream()
                .filter(journal -> !journal.getJournalDate().isBefore(query.startDate())
                        && !journal.getJournalDate().isAfter(query.endDate()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Journal> findByUserAndDate(GetJournalDetailQuery query) {
        return journals.stream()
                .filter(journal -> journal.getJournalDate().isEqual(query.date()))
                .findFirst();
    }
}
