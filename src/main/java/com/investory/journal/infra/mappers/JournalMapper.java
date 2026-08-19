package com.investory.journal.infra.mappers;

import com.investory.journal.infra.entities.JournalRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface JournalMapper {
    List<JournalRow> findByUserAndDateRange(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    int countByUserAndDateRange(@Param("userId") Long userId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    List<JournalRow> findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    List<JournalRow> findById(@Param("journalId") Long journalId);

    List<JournalRow> findByIds(@Param("journalIds") List<Long> journalIds);

    void insert(JournalRow row);

    void update(JournalRow row);

    void deleteByUserId(@Param("userId") Long userId);
}
