package com.investory.principle.domain.services;

import java.util.List;
import java.util.Map;

// principle_templates 테이블은 아직 없으므로(§13 미확정), 분석 유형 코드별 추천 후보 문구를
// 코드 상에 고정 매핑으로 둔다. tendency가 실제 지표 기반 추천을 산출하게 되면 이 클래스를
// TendencyAnalysisPort 응답의 세부 지표를 반영하는 로직으로 교체한다.
final class PrincipleRecommendationTemplates {

    record Template(String text, String reason, String ruleJson) {
    }

    private static final List<Template> DEFAULT_TEMPLATES = List.of(
            new Template(
                    "투자 근거를 작성한 후 매수한다.",
                    "매매 판단 근거를 기록하는 습관은 장기적인 투자 성과 점검에 도움이 됩니다.",
                    null)
    );

    private static final Map<String, List<Template>> TEMPLATES_BY_ANALYSIS_TYPE_CODE = Map.of(
            "CONCENTRATED", List.of(
                    new Template(
                            "한 종목의 투자 비중은 30%를 넘지 않는다.",
                            "특정 종목에 대한 편중된 투자가 관찰되어, 분산을 통해 위험을 낮출 것을 제안합니다.",
                            "{\"type\":\"MAX_POSITION_RATIO\",\"value\":30,\"unit\":\"PERCENT\"}"),
                    new Template(
                            "신규 종목 편입 전 최소 3영업일의 검토 기간을 갖는다.",
                            "충동적인 매수 경향이 관찰되어, 의사결정 전 숙려 기간을 갖는 것을 제안합니다.",
                            "{\"type\":\"MIN_REVIEW_DAYS\",\"value\":3,\"unit\":\"DAYS\"}")
            )
    );

    private PrincipleRecommendationTemplates() {
    }

    static List<Template> forAnalysisTypeCode(String analysisTypeCode) {
        return TEMPLATES_BY_ANALYSIS_TYPE_CODE.getOrDefault(analysisTypeCode, DEFAULT_TEMPLATES);
    }
}
