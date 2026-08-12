package com.investory.tendency.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
public class AnalysisRun {

    private final Long analysisRunId;
    private final Long userId;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final int tradeCount;
    private final int journalCount;
    private final String analysisVersion;
    private final Instant createdAt;

    private AnalysisRun(Long analysisRunId, Long userId, LocalDate periodStart, LocalDate periodEnd,
                         int tradeCount, int journalCount, String analysisVersion, Instant createdAt) {
        this.analysisRunId = analysisRunId;
        this.userId = userId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.tradeCount = tradeCount;
        this.journalCount = journalCount;
        this.analysisVersion = analysisVersion;
        this.createdAt = createdAt;
    }

    // 신규 실행 생성 시 사용. analysisRunId는 DB가 채워준다(insert 전 null).
    public static AnalysisRun create(Long userId, LocalDate periodStart, LocalDate periodEnd,
                                      int tradeCount, int journalCount, String analysisVersion) {
        return new AnalysisRun(null, userId, periodStart, periodEnd, tradeCount, journalCount, analysisVersion, null);
    }

    // DB에서 조회한 값으로 복원할 때 사용
    public static AnalysisRun of(Long analysisRunId, Long userId, LocalDate periodStart, LocalDate periodEnd,
                                  int tradeCount, int journalCount, String analysisVersion, Instant createdAt) {
        return new AnalysisRun(analysisRunId, userId, periodStart, periodEnd, tradeCount, journalCount, analysisVersion, createdAt);
    }
}
