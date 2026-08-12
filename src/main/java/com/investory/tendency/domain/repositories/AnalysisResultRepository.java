package com.investory.tendency.domain.repositories;

import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisResultDetail;

import java.util.List;
import java.util.Optional;

public interface AnalysisResultRepository {

    void saveAll(List<AnalysisResult> results);

    // 결과 상세 조회 화면용 — analysis_dimensions/analysis_types와 조인해 표시 이름까지 포함
    List<AnalysisResultDetail> findDetailByAnalysisRunId(Long analysisRunId);

    // analysisResultId 단건 조회 — principle 등 다른 도메인이 특정 결과 하나의 타입 정보를 알고 싶을 때 사용
    Optional<AnalysisResultDetail> findDetailById(Long analysisResultId);
}
