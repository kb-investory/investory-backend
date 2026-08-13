package com.investory.principle.domain.services.dto.command;

// tendency가 성향 분석을 완료(요청)한 시점에, 그 분석에 대응하는 추천 후보를 새로 생성하기 위한 입력.
// 향후 principle/infra/listeners의 이벤트 리스너가 tendency의 완료 이벤트를 이 커맨드로 변환해 호출한다.
// analysisTypeName은 LLM 추천 생성 프롬프트에 사람이 읽을 수 있는 성향명("집중투자형" 등)을 주기 위함 —
// analysis_types 테이블을 따로 조회하지 않고 이벤트로 이미 전달받은 값을 그대로 흘려보낸다.
public record RefreshRecommendationsCommand(
        Long analysisResultId,
        String analysisTypeCode,
        String analysisTypeName
) {
}
