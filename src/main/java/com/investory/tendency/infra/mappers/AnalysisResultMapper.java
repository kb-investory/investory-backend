package com.investory.tendency.infra.mappers;

import com.investory.tendency.infra.entities.AnalysisResultDetailRow;
import com.investory.tendency.infra.entities.AnalysisResultRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnalysisResultMapper {
    void insert(AnalysisResultRow row);

    List<AnalysisResultDetailRow> findDetailByAnalysisRunId(@Param("analysisRunId") Long analysisRunId);

    AnalysisResultDetailRow findDetailById(@Param("analysisResultId") Long analysisResultId);

    List<Long> findIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(@Param("userId") Long userId);
}
