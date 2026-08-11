package com.investory.tendency.infra.mappers;

import com.investory.tendency.infra.entities.RationaleLabelCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RationaleLabelStatsMapper {
    List<RationaleLabelCountRow> countByUserAndDateRange(@Param("userId") Long userId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);
}
