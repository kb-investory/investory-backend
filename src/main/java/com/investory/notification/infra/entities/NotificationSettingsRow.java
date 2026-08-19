package com.investory.notification.infra.entities;

import com.investory.notification.domain.model.NotificationSettings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class NotificationSettingsRow {
    private Long userId;
    private boolean tradeIngestedEnabled;
    private boolean tendencyAnalyzedEnabled;
    private boolean simulationCompletedEnabled;
    private Instant createdAt;
    private Instant updatedAt;

    public NotificationSettings toDomain() {
        return NotificationSettings.of(
                userId, tradeIngestedEnabled, tendencyAnalyzedEnabled, simulationCompletedEnabled, createdAt, updatedAt);
    }

    public static NotificationSettingsRow from(NotificationSettings settings) {
        NotificationSettingsRow row = new NotificationSettingsRow();
        row.userId = settings.getUserId();
        row.tradeIngestedEnabled = settings.isTradeIngestedEnabled();
        row.tendencyAnalyzedEnabled = settings.isTendencyAnalyzedEnabled();
        row.simulationCompletedEnabled = settings.isSimulationCompletedEnabled();
        row.createdAt = settings.getCreatedAt();
        row.updatedAt = settings.getUpdatedAt();
        return row;
    }
}
