package com.investory.journal.infra.mappers;

import com.investory.journal.infra.entities.JournalTradeNoteRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JournalTradeNoteMapper {
    List<JournalTradeNoteRow> findByTradeIds(List<Long> tradeIds);
    List<JournalTradeNoteRow> findByJournalId(@Param("journalId") Long journalId);
    void insertAll(@Param("notes") List<JournalTradeNoteRow> notes); // upsert — ON DUPLICATE KEY UPDATE
    void deleteByTradeIds(List<Long> tradeIds);
}
