package com.investory.tendency.infra.mappers;

import com.investory.tendency.infra.entities.AnalysisRunRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnalysisRunMapper {
    void insert(AnalysisRunRow row);

    List<AnalysisRunRow> findByUserId(@Param("userId") Long userId);

    AnalysisRunRow findByIdAndUserId(@Param("analysisRunId") Long analysisRunId, @Param("userId") Long userId);

    void deleteByUserId(@Param("userId") Long userId);
}
