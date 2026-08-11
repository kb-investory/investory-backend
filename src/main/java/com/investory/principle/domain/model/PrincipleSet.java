package com.investory.principle.domain.model;

import com.investory.principle.domain.constant.PrincipleSetStatus;
import com.investory.principle.domain.exception.PrincipleErrorCode;
import com.investory.principle.domain.exception.PrincipleException;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class PrincipleSet {

    private final Long principleSetId;
    private final Long userId;
    private final Long analysisRunId;
    private final int versionNo;
    private final PrincipleSetStatus status;
    private final List<PrincipleSetItem> items;
    private final Instant createdAt;
    private final Instant updatedAt;

    private PrincipleSet(Long principleSetId, Long userId, Long analysisRunId, int versionNo, PrincipleSetStatus status,
                          List<PrincipleSetItem> items, Instant createdAt, Instant updatedAt) {
        if (userId == null || status == null || items == null || createdAt == null || updatedAt == null) {
            throw new PrincipleException(PrincipleErrorCode.INVALID_PRINCIPLE_DATA);
        }

        this.principleSetId = principleSetId;
        this.userId = userId;
        this.analysisRunId = analysisRunId;
        this.versionNo = versionNo;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 신규 저장: 항상 ACTIVE 상태로 생성한다 — DRAFT 상태를 만드는 진입점은 아직 없다(§13 참고).
    public static PrincipleSet create(Long userId, Long analysisRunId, int versionNo, List<PrincipleSetItem> items) {
        Instant now = Instant.now();
        return new PrincipleSet(null, userId, analysisRunId, versionNo, PrincipleSetStatus.ACTIVE, items, now, now);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static PrincipleSet of(Long principleSetId, Long userId, Long analysisRunId, int versionNo, PrincipleSetStatus status,
                                   List<PrincipleSetItem> items, Instant createdAt, Instant updatedAt) {
        return new PrincipleSet(principleSetId, userId, analysisRunId, versionNo, status, items, createdAt, updatedAt);
    }
}
