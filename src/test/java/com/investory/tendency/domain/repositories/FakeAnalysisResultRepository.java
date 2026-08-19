package com.investory.tendency.domain.repositories;

import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisResultDetail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 실제 저장소는 analysis_dimensions/analysis_types와 조인해 표시 이름을 채우지만, 여기선 그 마스터
// 데이터가 없으므로 dimensionName/typeName은 코드값을 그대로 돌려준다(dimensionCode/typeCode) —
// 이 코드가 필요한 테스트들은 이름 텍스트 자체가 아니라 ID/코드 매핑을 확인하는 것이 목적이라 충분하다.
public class FakeAnalysisResultRepository implements AnalysisResultRepository {

    // 실제 analysis_results엔 user_id가 없어(analysis_run_id만) findIdsByUserId/deleteByUserId는
    // analysis_runs 조인으로 user_id를 얻는다. 이 페이크는 그 조인을 흉내내는 대신 userId를 직접
    // 들고 있는다 — saveAll()로 들어온 것(운영 코드 경로)은 userId를 모르니 null로 남고,
    // add()(탈퇴 테스트 전용 설정용)로 넣은 것만 findIdsByUserId/deleteByUserId 대상이 된다.
    private record Owned(Long userId, AnalysisResult result) {
    }

    private final List<Owned> results = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public void saveAll(List<AnalysisResult> newResults) {
        for (AnalysisResult result : newResults) {
            results.add(new Owned(null, AnalysisResult.of(nextId++, result.getAnalysisRunId(), result.getAnalysisDimensionCode(),
                    result.getPrimaryAnalysisTypeCode(), result.getEvidenceJson(), Instant.now())));
        }
    }

    public void add(Long userId, AnalysisResult result) {
        results.add(new Owned(userId, AnalysisResult.of(nextId++, result.getAnalysisRunId(), result.getAnalysisDimensionCode(),
                result.getPrimaryAnalysisTypeCode(), result.getEvidenceJson(), Instant.now())));
    }

    @Override
    public List<AnalysisResultDetail> findDetailByAnalysisRunId(Long analysisRunId) {
        return results.stream()
                .map(Owned::result)
                .filter(r -> r.getAnalysisRunId().equals(analysisRunId))
                .map(this::toDetail)
                .toList();
    }

    @Override
    public Optional<AnalysisResultDetail> findDetailById(Long analysisResultId) {
        return results.stream()
                .map(Owned::result)
                .filter(r -> r.getAnalysisResultId().equals(analysisResultId))
                .map(this::toDetail)
                .findFirst();
    }

    @Override
    public List<Long> findIdsByUserId(Long userId) {
        return results.stream()
                .filter(o -> userId.equals(o.userId()))
                .map(o -> o.result().getAnalysisResultId())
                .toList();
    }

    @Override
    public void deleteByUserId(Long userId) {
        results.removeIf(o -> userId.equals(o.userId()));
    }

    private AnalysisResultDetail toDetail(AnalysisResult result) {
        return new AnalysisResultDetail(result.getAnalysisResultId(), result.getAnalysisDimensionCode(),
                result.getAnalysisDimensionCode(), result.getPrimaryAnalysisTypeCode(),
                result.getPrimaryAnalysisTypeCode(), null, result.getEvidenceJson());
    }
}
