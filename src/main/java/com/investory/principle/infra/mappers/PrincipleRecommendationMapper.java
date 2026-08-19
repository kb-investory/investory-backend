package com.investory.principle.infra.mappers;

import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.infra.entities.PrincipleRecommendationRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrincipleRecommendationMapper {
    List<PrincipleRecommendationRow> findByAnalysisResultId(@Param("analysisResultId") Long analysisResultId);
    List<PrincipleRecommendationRow> findByIds(@Param("ids") List<Long> ids);
    void insert(PrincipleRecommendationRow row);
    void updateStatus(@Param("principleRecommendationId") Long principleRecommendationId,
                       @Param("recommendationStatus") RecommendationStatus recommendationStatus);
    void deleteByAnalysisResultIds(@Param("analysisResultIds") List<Long> analysisResultIds);
}
