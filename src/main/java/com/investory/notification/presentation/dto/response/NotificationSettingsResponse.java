package com.investory.notification.presentation.dto.response;

import com.investory.notification.domain.services.dto.result.NotificationSettingsResult;

public record NotificationSettingsResponse(
        boolean tradeIngestedEnabled,
        boolean tendencyAnalyzedEnabled,
        boolean simulationCompletedEnabled
) {
    public static NotificationSettingsResponse from(NotificationSettingsResult result) {
        return new NotificationSettingsResponse(
                result.tradeIngestedEnabled(), result.tendencyAnalyzedEnabled(), result.simulationCompletedEnabled());
    }
}
