package com.investory.notification.presentation.dto.request;

import com.investory.notification.domain.services.dto.command.UpdateNotificationSettingsCommand;

public record UpdateNotificationSettingsRequest(
        Boolean tradeIngestedEnabled,
        Boolean tendencyAnalyzedEnabled,
        Boolean simulationCompletedEnabled
) {
    public UpdateNotificationSettingsCommand toCommand(Long userId) {
        return new UpdateNotificationSettingsCommand(userId, tradeIngestedEnabled, tendencyAnalyzedEnabled, simulationCompletedEnabled);
    }
}
