package com.investory.notification.domain.model;

import com.investory.notification.domain.exception.NotificationErrorCode;
import com.investory.notification.domain.exception.NotificationException;
import lombok.Getter;

import java.time.Instant;

@Getter
public class NotificationSettings {

    private final Long userId;
    private final boolean tradeIngestedEnabled;
    private final boolean tendencyAnalyzedEnabled;
    private final boolean simulationCompletedEnabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    private NotificationSettings(Long userId, boolean tradeIngestedEnabled, boolean tendencyAnalyzedEnabled,
                                  boolean simulationCompletedEnabled, Instant createdAt, Instant updatedAt) {
        if (userId == null || createdAt == null || updatedAt == null) {
            throw new NotificationException(NotificationErrorCode.INVALID_SETTINGS_DATA);
        }
        this.userId = userId;
        this.tradeIngestedEnabled = tradeIngestedEnabled;
        this.tendencyAnalyzedEnabled = tendencyAnalyzedEnabled;
        this.simulationCompletedEnabled = simulationCompletedEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 아직 설정 행이 없는 사용자에게 보여줄 기본값(전부 수신). DB에는 반영하지 않는다 — PUT으로
    // 명시적으로 저장하기 전까지는 조회 응답에만 쓰이는 가상의 기본값이다. 회원가입 시 auth가
    // 이 기본값으로 실제 행을 만들어주는 연동(NotificationInitPort)이 붙기 전까지의 안전망이기도 하다.
    public static NotificationSettings defaults(Long userId) {
        Instant now = Instant.now();
        return new NotificationSettings(userId, true, true, true, now, now);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용).
    public static NotificationSettings of(Long userId, boolean tradeIngestedEnabled, boolean tendencyAnalyzedEnabled,
                                           boolean simulationCompletedEnabled, Instant createdAt, Instant updatedAt) {
        return new NotificationSettings(userId, tradeIngestedEnabled, tendencyAnalyzedEnabled, simulationCompletedEnabled, createdAt, updatedAt);
    }

    // 전체 교체 — 3개 필드를 항상 함께 갱신한다. userId/createdAt은 유지, updatedAt만 새로 찍는다.
    public NotificationSettings update(boolean tradeIngestedEnabled, boolean tendencyAnalyzedEnabled, boolean simulationCompletedEnabled) {
        return new NotificationSettings(userId, tradeIngestedEnabled, tendencyAnalyzedEnabled, simulationCompletedEnabled, createdAt, Instant.now());
    }
}
