package com.investory.principle.infra.repository_impls;

import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.domain.model.PrincipleRecommendation;
import com.investory.principle.domain.repositories.PrincipleRecommendationRepository;
import com.investory.principle.infra.entities.PrincipleRecommendationRow;
import com.investory.principle.infra.exception.PrincipleInfraException;
import com.investory.principle.infra.mappers.PrincipleRecommendationMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PrincipleRecommendationRepositoryImpl implements PrincipleRecommendationRepository {

    private final PrincipleRecommendationMapper principleRecommendationMapper;

    public PrincipleRecommendationRepositoryImpl(PrincipleRecommendationMapper principleRecommendationMapper) {
        this.principleRecommendationMapper = principleRecommendationMapper;
    }

    @Override
    public List<PrincipleRecommendation> findByAnalysisResultId(Long analysisResultId) {
        try {
            return principleRecommendationMapper.findByAnalysisResultId(analysisResultId).stream()
                    .map(PrincipleRecommendationRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("추천 원칙을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<PrincipleRecommendation> findByIds(List<Long> principleRecommendationIds) {
        if (principleRecommendationIds.isEmpty()) {
            return List.of();
        }
        try {
            return principleRecommendationMapper.findByIds(principleRecommendationIds).stream()
                    .map(PrincipleRecommendationRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("추천 원칙을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<PrincipleRecommendation> saveAll(List<PrincipleRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return List.of();
        }
        try {
            List<PrincipleRecommendation> saved = new ArrayList<>();
            for (PrincipleRecommendation recommendation : recommendations) {
                PrincipleRecommendationRow row = PrincipleRecommendationRow.from(recommendation);
                principleRecommendationMapper.insert(row);
                saved.add(row.toDomain());
            }
            return saved;
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("추천 원칙을 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void updateStatus(Long principleRecommendationId, RecommendationStatus status) {
        try {
            principleRecommendationMapper.updateStatus(principleRecommendationId, status);
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("추천 원칙 상태를 변경하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteByAnalysisResultIds(List<Long> analysisResultIds) {
        if (analysisResultIds.isEmpty()) {
            return;
        }
        try {
            principleRecommendationMapper.deleteByAnalysisResultIds(analysisResultIds);
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("추천 원칙을 삭제하는 중 오류가 발생했습니다.", e);
        }
    }
}
