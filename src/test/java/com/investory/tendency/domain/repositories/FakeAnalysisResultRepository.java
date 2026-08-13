package com.investory.tendency.domain.repositories;

import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisResultDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 실제 저장소는 analysis_dimensions/analysis_types와 조인해 표시 이름을 채우지만, 여기선 그 마스터
// 데이터가 없으므로 dimensionName/typeName은 코드값을 그대로 돌려준다(dimensionCode/typeCode) —
// 이 코드가 필요한 테스트들은 이름 텍스트 자체가 아니라 ID/코드 매핑을 확인하는 것이 목적이라 충분하다.
public class FakeAnalysisResultRepository implements AnalysisResultRepository {

    private final List<AnalysisResult> results = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public void saveAll(List<AnalysisResult> newResults) {
        for (AnalysisResult result : newResults) {
            results.add(AnalysisResult.of(nextId++, result.getAnalysisRunId(), result.getAnalysisDimensionCode(),
                    result.getPrimaryAnalysisTypeCode(), result.getEvidenceJson(), java.time.Instant.now()));
        }
    }

    @Override
    public List<AnalysisResultDetail> findDetailByAnalysisRunId(Long analysisRunId) {
        return results.stream()
                .filter(r -> r.getAnalysisRunId().equals(analysisRunId))
                .map(this::toDetail)
                .toList();
    }

    @Override
    public Optional<AnalysisResultDetail> findDetailById(Long analysisResultId) {
        return results.stream()
                .filter(r -> r.getAnalysisResultId().equals(analysisResultId))
                .map(this::toDetail)
                .findFirst();
    }

    private AnalysisResultDetail toDetail(AnalysisResult result) {
        return new AnalysisResultDetail(result.getAnalysisResultId(), result.getAnalysisDimensionCode(),
                result.getAnalysisDimensionCode(), result.getPrimaryAnalysisTypeCode(),
                result.getPrimaryAnalysisTypeCode(), null, result.getEvidenceJson());
    }
}
