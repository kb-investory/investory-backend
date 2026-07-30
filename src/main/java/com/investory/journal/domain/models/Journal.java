package com.investory.journal.domain.models;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
public class Journal {

    private final Long journalId;
    private final LocalDate journalDate;
    private final String marketThought;
    private final MarketMood marketMood;
    private final int tradeNoteCount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant editableUntilAt;

    private Journal(Long journalId, LocalDate journalDate, String marketThought, MarketMood marketMood,
                     int tradeNoteCount, Instant createdAt, Instant updatedAt, Instant editableUntilAt) {
        requireNonNull(journalId);
        requireNonNull(journalDate);
        requireNonNull(marketThought);
        requireNonNull(createdAt);
        requireNonNull(updatedAt);
        requireNonNull(editableUntilAt);

        this.journalId = journalId;
        this.journalDate = journalDate;
        this.marketThought = marketThought;
        this.marketMood = marketMood;
        this.tradeNoteCount = tradeNoteCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.editableUntilAt = editableUntilAt;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new JournalException(JournalErrorCode.INVALID_JOURNAL_DATA);
        }
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용). marketMood는 선택 입력이라 null 허용.
    //
    // 목록 조회(findByUserAndDateRange)와 상세 조회(findByUserAndDate) 양쪽 모두 이 팩토리로
    // 항상 8개 필드를 전부 채워서 복원한다 — 조회 경로에 따라 일부 필드만 채워진 반쪽짜리
    // 애그리게잇이 존재하면, 나중에 목록 조회 결과에서 marketThought를 잘못 참조해 "비어있다"고
    // 오인하는 버그가 생길 수 있어서다. 지금은 두 컬럼(market_thought, updated_at)이 같은 행에서
    // 추가 JOIN 없이 더 SELECT하면 되는 수준이라 이 방식이 싸지만, 나중에 세 번째 조회 패턴이
    // 생기고 그 필드가 계산 비용이 크다면 그때는 읽기 전용 축소 모델(JournalSummary 등)을
    // 별도로 두는 쪽을 기본값으로 검토할 것.
    public static Journal of(Long journalId, LocalDate journalDate, String marketThought, MarketMood marketMood,
                              int tradeNoteCount, Instant createdAt, Instant updatedAt, Instant editableUntilAt) {
        return new Journal(journalId, journalDate, marketThought, marketMood, tradeNoteCount, createdAt, updatedAt, editableUntilAt);
    }
}
