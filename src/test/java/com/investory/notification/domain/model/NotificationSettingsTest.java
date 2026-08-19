package com.investory.notification.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationSettingsTest {

    @Test
    void defaults는_전부_수신함이다() {
        NotificationSettings settings = NotificationSettings.defaults(1L);

        assertTrue(settings.isTradeIngestedEnabled());
        assertTrue(settings.isTendencyAnalyzedEnabled());
        assertTrue(settings.isSimulationCompletedEnabled());
    }

    @Test
    void update하면_3개_필드가_모두_바뀌고_userId와_createdAt은_유지된다() throws InterruptedException {
        NotificationSettings settings = NotificationSettings.defaults(1L);

        Thread.sleep(2);
        NotificationSettings updated = settings.update(false, false, true);

        assertEquals(1L, updated.getUserId());
        assertEquals(settings.getCreatedAt(), updated.getCreatedAt());
        assertTrue(updated.getUpdatedAt().isAfter(settings.getUpdatedAt()));
        assertEquals(false, updated.isTradeIngestedEnabled());
        assertEquals(false, updated.isTendencyAnalyzedEnabled());
        assertTrue(updated.isSimulationCompletedEnabled());
    }
}
