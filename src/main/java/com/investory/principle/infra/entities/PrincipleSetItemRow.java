package com.investory.principle.infra.entities;

import com.investory.principle.domain.model.PrincipleSetItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PrincipleSetItemRow {
    private Long principleSetItemId;
    private Long principleSetId;
    private Long principleRecommendationId;
    private String principleText;
    private String ruleJson;
    private int sortOrder;

    public PrincipleSetItem toDomain() {
        return PrincipleSetItem.of(principleSetItemId, principleRecommendationId, principleText, ruleJson, sortOrder);
    }

    // 세트 insert 이후에야 principleSetId가 정해지므로 여기서 주입받는다 — 아이템 모델 자체는 아직 모른다.
    public static PrincipleSetItemRow from(PrincipleSetItem item, Long principleSetId) {
        PrincipleSetItemRow row = new PrincipleSetItemRow();
        row.principleSetItemId = item.getPrincipleSetItemId();
        row.principleSetId = principleSetId;
        row.principleRecommendationId = item.getPrincipleRecommendationId();
        row.principleText = item.getPrincipleText();
        row.ruleJson = item.getRuleJson();
        row.sortOrder = item.getSortOrder();
        return row;
    }
}
