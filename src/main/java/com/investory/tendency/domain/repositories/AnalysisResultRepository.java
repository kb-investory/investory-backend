package com.investory.tendency.domain.repositories;

import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisResultDetail;

import java.util.List;

public interface AnalysisResultRepository {

    void saveAll(List<AnalysisResult> results);

    // 결과 상세 조회 화면용 — analysis_dimensions/analysis_types와 조인해 표시 이름까지 포함
    List<AnalysisResultDetail> findDetailByAnalysisRunId(Long analysisRunId);
}
