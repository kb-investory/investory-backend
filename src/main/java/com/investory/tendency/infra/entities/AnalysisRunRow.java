package com.investory.tendency.infra.entities;

import com.investory.tendency.domain.model.AnalysisRun;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class AnalysisRunRow {
    private Long analysisRunId;
    private Long userId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int tradeCount;
    private int journalCount;
    private String analysisVersion;
    private Instant createdAt;

    public static AnalysisRunRow from(AnalysisRun run) {
        AnalysisRunRow row = new AnalysisRunRow();
        row.analysisRunId = run.getAnalysisRunId();
        row.userId = run.getUserId();
        row.periodStart = run.getPeriodStart();
        row.periodEnd = run.getPeriodEnd();
        row.tradeCount = run.getTradeCount();
        row.journalCount = run.getJournalCount();
        row.analysisVersion = run.getAnalysisVersion();
        row.createdAt = run.getCreatedAt();
        return row;
    }

    public AnalysisRun toDomain() {
        return AnalysisRun.of(analysisRunId, userId, periodStart, periodEnd, tradeCount, journalCount, analysisVersion, createdAt);
    }
}
