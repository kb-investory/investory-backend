package com.investory.tendency.infra.repository_impls;

import com.investory.tendency.domain.model.AnalysisRun;
import com.investory.tendency.domain.repositories.AnalysisRunRepository;
import com.investory.tendency.infra.entities.AnalysisRunRow;
import com.investory.tendency.infra.exception.TendencyInfraException;
import com.investory.tendency.infra.mappers.AnalysisRunMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AnalysisRunRepositoryImpl implements AnalysisRunRepository {

    private final AnalysisRunMapper analysisRunMapper;

    public AnalysisRunRepositoryImpl(AnalysisRunMapper analysisRunMapper) {
        this.analysisRunMapper = analysisRunMapper;
    }

    @Override
    public AnalysisRun save(AnalysisRun analysisRun) {
        try {
            AnalysisRunRow row = AnalysisRunRow.from(analysisRun);
            analysisRunMapper.insert(row);
            return row.toDomain();
        } catch (DataAccessException e) {
            throw new TendencyInfraException("성향 분석 실행 결과를 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<AnalysisRun> findByUserId(Long userId) {
        try {
            return analysisRunMapper.findByUserId(userId).stream()
                    .map(AnalysisRunRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new TendencyInfraException("성향 분석 이력을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<AnalysisRun> findByIdAndUserId(Long analysisRunId, Long userId) {
        try {
            AnalysisRunRow row = analysisRunMapper.findByIdAndUserId(analysisRunId, userId);
            return Optional.ofNullable(row).map(AnalysisRunRow::toDomain);
        } catch (DataAccessException e) {
            throw new TendencyInfraException("성향 분석 실행 결과를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}
