package com.investory.tendency.domain.repositories;


import com.investory.tendency.domain.constant.RationaleLabelType;

import java.time.LocalDate;
import java.util.Map;

public interface RationaleLabelStatsRepository {

    // 지정한 사용자의 [startDate, endDate] 구간(journal_date 기준, 양 끝 포함)에 속하는
    // journal_trade_notes.rationale_label만 라벨별로 COUNT해서 반환한다.
    // 해당 구간에 존재하지 않는 라벨은 결과 Map에 아예 들어있지 않을 수 있다 — 호출 측에서 0으로 취급해야 한다.
    Map<RationaleLabelType, Long> countByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}
