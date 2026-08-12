package com.investory.tendency.infra.repository_impls;

import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisResultDetail;
import com.investory.tendency.domain.repositories.AnalysisResultRepository;
import com.investory.tendency.infra.entities.AnalysisResultRow;
import com.investory.tendency.infra.exception.TendencyInfraException;
import com.investory.tendency.infra.mappers.AnalysisResultMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AnalysisResultRepositoryImpl implements AnalysisResultRepository {

    private final AnalysisResultMapper analysisResultMapper;

    public AnalysisResultRepositoryImpl(AnalysisResultMapper analysisResultMapper) {
        this.analysisResultMapper = analysisResultMapper;
    }

    // 여러 건이지만 실행당 최대 6건 수준이라 배치 없이 개별 insert로 충분하다.
    @Override
    public void saveAll(List<AnalysisResult> results) {
        try {
            for (AnalysisResult result : results) {
                analysisResultMapper.insert(AnalysisResultRow.from(result));
            }
        } catch (DataAccessException e) {
            throw new TendencyInfraException("성향 분석 결과를 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<AnalysisResultDetail> findDetailByAnalysisRunId(Long analysisRunId) {
        try {
            return analysisResultMapper.findDetailByAnalysisRunId(analysisRunId).stream()
                    .map(row -> row.toDomain())
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new TendencyInfraException("성향 분석 결과를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}
