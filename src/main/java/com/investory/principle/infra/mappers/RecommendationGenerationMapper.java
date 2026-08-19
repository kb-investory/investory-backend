package com.investory.principle.infra.mappers;

import com.investory.principle.infra.entities.RecommendationGenerationRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecommendationGenerationMapper {
    List<RecommendationGenerationRow> findByAnalysisRunId(@Param("analysisRunId") Long analysisRunId);
    void upsert(RecommendationGenerationRow row);
}
