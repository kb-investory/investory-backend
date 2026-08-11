package com.investory.principle.domain.model;

import com.investory.principle.domain.exception.PrincipleErrorCode;
import com.investory.principle.domain.exception.PrincipleException;
import lombok.Getter;

@Getter
public class PrincipleSetItem {

    private final Long principleSetItemId;
    private final Long principleRecommendationId;
    private final String principleText;
    private final String ruleJson;
    private final int sortOrder;

    private PrincipleSetItem(Long principleSetItemId, Long principleRecommendationId, String principleText,
                              String ruleJson, int sortOrder) {
        if (principleText == null || principleText.isBlank()) {
            throw new PrincipleException(PrincipleErrorCode.INVALID_PRINCIPLE_DATA);
        }

        this.principleSetItemId = principleSetItemId;
        this.principleRecommendationId = principleRecommendationId;
        this.principleText = principleText;
        this.ruleJson = ruleJson;
        this.sortOrder = sortOrder;
    }

    // 신규 저장: principleSetItemId는 아직 없고(DB가 생성), 소속 principleSetId는 세트 insert 이후에야
    // 정해지므로 이 모델은 세트에 속하기 전 상태를 표현한다 — principleSetId는 Row 변환 시점에 주입한다.
    public static PrincipleSetItem create(Long principleRecommendationId, String principleText, String ruleJson, int sortOrder) {
        return new PrincipleSetItem(null, principleRecommendationId, principleText, ruleJson, sortOrder);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static PrincipleSetItem of(Long principleSetItemId, Long principleRecommendationId, String principleText,
                                       String ruleJson, int sortOrder) {
        return new PrincipleSetItem(principleSetItemId, principleRecommendationId, principleText, ruleJson, sortOrder);
    }

    public boolean isAiRecommendation() {
        return principleRecommendationId != null;
    }
}
