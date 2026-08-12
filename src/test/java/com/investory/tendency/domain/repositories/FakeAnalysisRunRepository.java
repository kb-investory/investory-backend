package com.investory.tendency.domain.repositories;

import com.investory.tendency.domain.model.AnalysisRun;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FakeAnalysisRunRepository implements AnalysisRunRepository {

    private final List<AnalysisRun> runs = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public AnalysisRun save(AnalysisRun analysisRun) {
        // Instant.now()는 빠른 연속 저장 시 같은 값이 나올 수 있어, 저장 순서를 확실히 구분하려고
        // nextId를 초 단위로 더해 createdAt을 만든다 — findByUserId의 "최신순" 정렬을 테스트에서 신뢰 가능하게 함.
        Instant createdAt = Instant.EPOCH.plusSeconds(nextId);
        AnalysisRun saved = AnalysisRun.of(nextId++, analysisRun.getUserId(), analysisRun.getPeriodStart(),
                analysisRun.getPeriodEnd(), analysisRun.getTradeCount(), analysisRun.getJournalCount(),
                analysisRun.getAnalysisVersion(), createdAt);
        runs.add(saved);
        return saved;
    }

    @Override
    public List<AnalysisRun> findByUserId(Long userId) {
        return runs.stream()
                .filter(r -> r.getUserId().equals(userId))
                .sorted(Comparator.comparing(AnalysisRun::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public Optional<AnalysisRun> findByIdAndUserId(Long analysisRunId, Long userId) {
        return runs.stream()
                .filter(r -> r.getAnalysisRunId().equals(analysisRunId) && r.getUserId().equals(userId))
                .findFirst();
    }
}
