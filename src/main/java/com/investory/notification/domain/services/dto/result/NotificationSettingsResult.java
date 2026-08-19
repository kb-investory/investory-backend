package com.investory.notification.domain.services.dto.result;

import com.investory.notification.domain.model.NotificationSettings;

public record NotificationSettingsResult(
        boolean tradeIngestedEnabled,
        boolean tendencyAnalyzedEnabled,
        boolean simulationCompletedEnabled
) {
    public static NotificationSettingsResult from(NotificationSettings settings) {
        return new NotificationSettingsResult(
                settings.isTradeIngestedEnabled(),
                settings.isTendencyAnalyzedEnabled(),
                settings.isSimulationCompletedEnabled()
        );
    }
}
