package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;

import java.util.List;

public interface JournalRepository {
    List<Journal> findByUserAndDateRange(GetJournalEntriesQuery query);
}
