package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.Journal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalRepository {
    List<Journal> findByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    Optional<Journal> findByUserAndDate(Long userId, LocalDate date);
}
