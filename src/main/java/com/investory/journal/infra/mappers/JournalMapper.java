package com.investory.journal.infra.mappers;

import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.infra.entities.JournalRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JournalMapper {
    List<JournalRow> findByUserAndDateRange(GetJournalEntriesQuery query);
}
