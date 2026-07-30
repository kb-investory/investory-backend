package com.investory.journal.infra.repository_impls;

import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.repositories.JournalRepository;
import com.investory.journal.infra.entities.JournalRow;
import com.investory.journal.infra.exception.JournalInfraException;
import com.investory.journal.infra.mappers.JournalMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JournalRepositoryImpl implements JournalRepository {

    private final JournalMapper journalMapper;

    public JournalRepositoryImpl(JournalMapper journalMapper) {
        this.journalMapper = journalMapper;
    }

    @Override
    public List<Journal> findByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            return journalMapper.findByUserAndDateRange(userId, startDate, endDate).stream()
                    .map(JournalRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new JournalInfraException(e);
        }
    }

    @Override
    public Optional<Journal> findByUserAndDate(Long userId, LocalDate date) {
        try {
            return journalMapper.findByUserAndDate(userId, date).stream()
                    .map(JournalRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new JournalInfraException("특정 날짜의 투자일지를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}
