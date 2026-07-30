package com.investory.journal.infra.mappers;

import com.investory.journal.infra.entities.JournalTradeNoteRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JournalTradeNoteMapper {
    List<JournalTradeNoteRow> findByTradeIds(List<Long> tradeIds);
}
