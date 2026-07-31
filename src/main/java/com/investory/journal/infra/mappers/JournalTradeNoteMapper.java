package com.investory.journal.infra.mappers;

import com.investory.journal.infra.entities.JournalTradeNoteRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JournalTradeNoteMapper {
    List<JournalTradeNoteRow> findByTradeIds(List<Long> tradeIds);
    void insertAll(@Param("notes") List<JournalTradeNoteRow> notes);
}
