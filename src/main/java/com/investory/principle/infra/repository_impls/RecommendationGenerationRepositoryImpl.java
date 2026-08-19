package com.investory.principle.infra.repository_impls;

import com.investory.principle.domain.model.RecommendationGeneration;
import com.investory.principle.domain.repositories.RecommendationGenerationRepository;
import com.investory.principle.infra.entities.RecommendationGenerationRow;
import com.investory.principle.infra.exception.PrincipleInfraException;
import com.investory.principle.infra.mappers.RecommendationGenerationMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RecommendationGenerationRepositoryImpl implements RecommendationGenerationRepository {

    private final RecommendationGenerationMapper recommendationGenerationMapper;

    public RecommendationGenerationRepositoryImpl(RecommendationGenerationMapper recommendationGenerationMapper) {
        this.recommendationGenerationMapper = recommendationGenerationMapper;
    }

    @Override
    public Optional<RecommendationGeneration> findByAnalysisRunId(Long analysisRunId) {
        try {
            return recommendationGenerationMapper.findByAnalysisRunId(analysisRunId).stream()
                    .findFirst()
                    .map(RecommendationGenerationRow::toDomain);
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("추천 생성 상태를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void save(RecommendationGeneration generation) {
        try {
            recommendationGenerationMapper.upsert(RecommendationGenerationRow.from(generation));
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("추천 생성 상태를 저장하는 중 오류가 발생했습니다.", e);
        }
    }
}
