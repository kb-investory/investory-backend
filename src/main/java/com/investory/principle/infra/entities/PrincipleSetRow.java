package com.investory.principle.infra.entities;

import com.investory.principle.domain.constant.PrincipleSetStatus;
import com.investory.principle.domain.model.PrincipleSet;
import com.investory.principle.domain.model.PrincipleSetItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PrincipleSetRow {
    private Long principleSetId;
    private Long userId;
    private Long analysisRunId;
    private int versionNo;
    private PrincipleSetStatus setStatus;
    private Instant createdAt;
    private Instant updatedAt;

    // items는 principle_set_items 테이블에서 별도 조회하므로 이 Row 자체에는 담지 않고,
    // 조합은 호출부(repository_impl)에서 담당한다.
    public PrincipleSet toDomain(List<PrincipleSetItem> items) {
        return PrincipleSet.of(principleSetId, userId, analysisRunId, versionNo, setStatus, items, createdAt, updatedAt);
    }

    public static PrincipleSetRow from(PrincipleSet principleSet) {
        PrincipleSetRow row = new PrincipleSetRow();
        row.principleSetId = principleSet.getPrincipleSetId();
        row.userId = principleSet.getUserId();
        row.analysisRunId = principleSet.getAnalysisRunId();
        row.versionNo = principleSet.getVersionNo();
        row.setStatus = principleSet.getStatus();
        row.createdAt = principleSet.getCreatedAt();
        row.updatedAt = principleSet.getUpdatedAt();
        return row;
    }
}
