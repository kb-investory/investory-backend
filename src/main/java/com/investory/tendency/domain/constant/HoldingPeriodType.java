package com.investory.tendency.domain.constant;

// 최근 90일 trade_matches.holding_days 분포로 결정되는 투자 기간 성향.
public enum HoldingPeriodType {
    SHORT_TERM("단기회전형"),   // 보유일 5일 이하 매칭이 최빈
    MEDIUM_TERM("중기보유형"),  // 보유일 6~30일 매칭이 최빈
    LONG_TERM("장기투자형"),    // 보유일 30일 초과 매칭이 최빈
    MIXED("혼합기간형");        // 최빈 구간 비율이 판정 기준선(threshold) 미만

    private final String labelName;

    HoldingPeriodType(String labelName) {
        this.labelName = labelName;
    }

    public String getLabelName() {
        return labelName;
    }
}
