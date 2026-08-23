package com.investory.tendency.domain.repositories;

import com.investory.tendency.domain.constant.AnalysisRunStatus;
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
                analysisRun.getAnalysisVersion(), analysisRun.getRunStatus(), analysisRun.getErrorMessage(), createdAt);
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

    @Override
    public void deleteByUserId(Long userId) {
        runs.removeIf(r -> r.getUserId().equals(userId));
    }

    @Override
    public void markRunning(Long analysisRunId) {
        replace(analysisRunId, r -> AnalysisRun.of(r.getAnalysisRunId(), r.getUserId(), r.getPeriodStart(), r.getPeriodEnd(),
                r.getTradeCount(), r.getJournalCount(), r.getAnalysisVersion(), AnalysisRunStatus.RUNNING, r.getErrorMessage(), r.getCreatedAt()));
    }

    @Override
    public void markSuccess(Long analysisRunId, int tradeCount, int journalCount) {
        replace(analysisRunId, r -> AnalysisRun.of(r.getAnalysisRunId(), r.getUserId(), r.getPeriodStart(), r.getPeriodEnd(),
                tradeCount, journalCount, r.getAnalysisVersion(), AnalysisRunStatus.SUCCESS, null, r.getCreatedAt()));
    }

    @Override
    public void markFailed(Long analysisRunId, String errorMessage) {
        replace(analysisRunId, r -> AnalysisRun.of(r.getAnalysisRunId(), r.getUserId(), r.getPeriodStart(), r.getPeriodEnd(),
                r.getTradeCount(), r.getJournalCount(), r.getAnalysisVersion(), AnalysisRunStatus.FAILED, errorMessage, r.getCreatedAt()));
    }

    @Override
    public boolean existsInProgressByUserId(Long userId) {
        return runs.stream()
                .anyMatch(r -> r.getUserId().equals(userId)
                        && (r.getRunStatus() == AnalysisRunStatus.REQUESTED || r.getRunStatus() == AnalysisRunStatus.RUNNING));
    }

    private void replace(Long analysisRunId, java.util.function.UnaryOperator<AnalysisRun> transform) {
        for (int i = 0; i < runs.size(); i++) {
            if (runs.get(i).getAnalysisRunId().equals(analysisRunId)) {
                runs.set(i, transform.apply(runs.get(i)));
                return;
            }
        }
    }
}
