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

    void markRunning(@Param("analysisRunId") Long analysisRunId);

    void markSuccess(@Param("analysisRunId") Long analysisRunId,
                      @Param("tradeCount") int tradeCount, @Param("journalCount") int journalCount);

    void markFailed(@Param("analysisRunId") Long analysisRunId, @Param("errorMessage") String errorMessage);

    boolean existsInProgressByUserId(@Param("userId") Long userId);
}
