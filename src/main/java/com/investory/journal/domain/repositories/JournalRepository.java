package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;

import java.util.List;
import java.util.Optional;

public interface JournalRepository {
    List<Journal> findByUserAndDateRange(GetJournalEntriesQuery query);
    Optional<Journal> findByUserAndDate(GetJournalDetailQuery query);
}
