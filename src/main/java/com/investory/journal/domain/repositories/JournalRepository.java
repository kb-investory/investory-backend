package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.Journal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalRepository {
    List<Journal> findByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    int countByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    Optional<Journal> findByUserAndDate(Long userId, LocalDate date);
    Optional<Journal> findById(Long journalId);
    List<Journal> findByIds(List<Long> journalIds);
    Journal save(Journal journal);
    void update(Journal journal);

    // 계정 탈퇴 시 — 호출 전에 이 사용자의 거래(및 그 거래에 달린 journal_trade_notes)가
    // 이미 지워졌다고 가정한다 (AuthService.withdraw()의 정리 순서 참고).
    void deleteByUserId(Long userId);
}
