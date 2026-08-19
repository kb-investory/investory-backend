package com.investory.notification.domain.services.dto.command;

// 전체 교체 API라 3개 필드 모두 필수다. Boolean(nullable)으로 받아 서비스에서 누락 여부를 검증한다.
public record UpdateNotificationSettingsCommand(
        Long userId,
        Boolean tradeIngestedEnabled,
        Boolean tendencyAnalyzedEnabled,
        Boolean simulationCompletedEnabled
) {
}
