package com.investory.notification.domain.model;

import com.investory.notification.domain.constant.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTest {

    @Test
    void 생성하면_읽지_않은_상태다() {
        Notification notification = Notification.create(1L, NotificationType.TRADE_INGESTED, "제목", "내용", 10L);

        assertFalse(notification.isRead());
        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void 읽지_않은_알림을_markAsRead하면_읽음으로_바뀌고_readAt이_채워진다() {
        Notification notification = Notification.of(
                1L, 100L, NotificationType.TENDENCY_ANALYZED, "제목", "내용", 20L, false, Instant.now(), null);

        Notification updated = notification.markAsRead(Instant.now());

        assertTrue(updated.isRead());
        assertNotNull(updated.getReadAt());
    }

    @Test
    void 이미_읽은_알림을_다시_markAsRead하면_기존_readAt을_그대로_유지한다() {
        Instant firstReadAt = Instant.now().minusSeconds(60);
        Notification notification = Notification.of(
                1L, 100L, NotificationType.TENDENCY_ANALYZED, "제목", "내용", 20L, true, Instant.now().minusSeconds(120), firstReadAt);

        Notification updated = notification.markAsRead(Instant.now());

        assertSame(notification, updated);
        assertEquals(firstReadAt, updated.getReadAt());
    }
}
