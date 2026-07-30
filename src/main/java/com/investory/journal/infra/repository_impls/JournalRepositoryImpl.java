package com.investory.journal.infra.repository_impls;

import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.repositories.JournalRepository;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.infra.entities.JournalRow;
import com.investory.journal.infra.exception.JournalInfraException;
import com.investory.journal.infra.mappers.JournalMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class JournalRepositoryImpl implements JournalRepository {

    private final JournalMapper journalMapper;

    public JournalRepositoryImpl(JournalMapper journalMapper) {
        this.journalMapper = journalMapper;
    }

    @Override
    public List<Journal> findByUserAndDateRange(GetJournalEntriesQuery query) {
        try {
            return journalMapper.findByUserAndDateRange(query).stream()
                    .map(JournalRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new JournalInfraException(e);
        }
    }
}
