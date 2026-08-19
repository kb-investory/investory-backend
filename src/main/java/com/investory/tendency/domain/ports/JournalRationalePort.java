package com.investory.tendency.domain.ports;

import java.time.LocalDate;

// journal 도메인이 소유한 investment_journals를 tendency 쪽에서 필요로 하는 모양대로 요청하는 포트.
// 구현체(JournalRationalePortImpl)는 journal.domain.repositories.JournalRepository를 통해서만
// 데이터를 받아온다 — journal의 테이블/SQL을 직접 알지 않는다.
public interface JournalRationalePort {

    // 지정한 사용자의 [startDate, endDate] 구간(journal_date 기준, 양 끝 포함)에 작성된 investment_journals 건수.
    int countJournalsInRange(Long userId, LocalDate startDate, LocalDate endDate);
}
