package com.investory.tendency.domain.model;

// analysis_results를 analysis_dimensions/analysis_types와 조인한 읽기 전용 축소 모델.
// 결과 상세 조회 화면에 필요한 표시용 이름까지 한 번에 담아서, 조회 쪽에서 매번
// 마스터 테이블을 따로 안 찾아도 되게 한다. 쓰기 작업에는 쓰지 않는다(AnalysisResult가 담당).
public record AnalysisResultDetail(
    Long analysisResultId,
    String dimensionCode,
    String dimensionName,
    String typeCode,
    String typeName,
    String typeDescription,
    String evidenceJson
) {
}
