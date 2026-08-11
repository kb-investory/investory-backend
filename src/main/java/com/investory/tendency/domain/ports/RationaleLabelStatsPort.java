package com.investory.tendency.domain.ports;


import com.investory.tendency.domain.constant.RationaleLabelType;

import java.time.LocalDate;
import java.util.Map;

// journal 도메인이 소유한 journal_trade_notes.rationale_label 집계를 tendency 쪽에서 필요로 하는 모양대로 요청하는 포트.
// journal.domain.JournalTradeNote는 rationale_label을 도메인 모델로 노출하지 않으므로(rationale_text만 보유),
// 구현체(RationaleLabelStatsPortImpl)가 journal의 테이블을 직접 읽는다 — journal 도메인 모델/리포지토리는 건드리지 않는다.
// journal이 이 필드를 도메인 API로 노출하게 되면 구현체만 교체하면 된다.
public interface RationaleLabelStatsPort {

    // 지정한 사용자의 [startDate, endDate] 구간(journal_date 기준, 양 끝 포함)에 속하는
    // journal_trade_notes.rationale_label만 라벨별로 COUNT해서 반환한다.
    // 해당 구간에 존재하지 않는 라벨은 결과 Map에 아예 들어있지 않을 수 있다 — 호출 측에서 0으로 취급해야 한다.
    Map<RationaleLabelType, Long> countByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}
