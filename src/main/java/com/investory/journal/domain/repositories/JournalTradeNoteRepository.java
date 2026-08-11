package com.investory.journal.domain.repositories;

import com.investory.journal.domain.models.JournalTradeNote;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface JournalTradeNoteRepository {
    List<JournalTradeNote> findByTradeIds(List<Long> tradeIds);
    List<JournalTradeNote> findByJournalId(Long journalId);
    void saveAll(List<JournalTradeNote> notes); // upsert — 있으면 갱신, 없으면 생성
    void deleteByTradeIds(List<Long> tradeIds);

    // 지정한 사용자의 [startDate, endDate] 구간(journal_date 기준, 양 끝 포함)에 속하는 매매노트를
    // rationale_label_type 원본 문자열별로 COUNT해서 반환한다. 대소문자 정규화나 enum 변환은 하지 않고
    // DB에 저장된 문자열을 그대로 키로 사용한다 — 그 해석은 이 값을 사용하는 쪽(tendency)의 책임이다.
    // 해당 구간에 존재하지 않는 라벨은 결과 Map에 아예 들어있지 않을 수 있다.
    Map<String, Long> countRationaleLabelsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}
