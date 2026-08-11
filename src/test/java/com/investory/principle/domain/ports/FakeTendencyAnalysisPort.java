package com.investory.principle.domain.ports;

import com.investory.principle.domain.ports.dto.TendencyAnalysisInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeTendencyAnalysisPort implements TendencyAnalysisPort {

    private final List<TendencyAnalysisInfo> latestCompletedAnalysisResults = new ArrayList<>();

    // 최신 실행의 항목별 결과를 하나씩 추가한다. 실행 1건에 결과가 여러 개(항목별)일 수 있으므로 여러 번 호출 가능.
    public void addLatestCompletedAnalysisResult(TendencyAnalysisInfo info) {
        this.latestCompletedAnalysisResults.add(info);
    }

    @Override
    public List<TendencyAnalysisInfo> findLatestCompletedAnalysisResults(Long userId) {
        return latestCompletedAnalysisResults;
    }

    @Override
    public Optional<TendencyAnalysisInfo> findAnalysisType(Long analysisResultId) {
        return latestCompletedAnalysisResults.stream()
                .filter(info -> info.analysisResultId().equals(analysisResultId))
                .findFirst();
    }
}
