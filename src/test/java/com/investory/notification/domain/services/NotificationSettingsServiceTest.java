package com.investory.notification.domain.services;

import com.investory.notification.domain.exception.NotificationErrorCode;
import com.investory.notification.domain.exception.NotificationException;
import com.investory.notification.domain.model.NotificationSettings;
import com.investory.notification.domain.repositories.FakeNotificationSettingsRepository;
import com.investory.notification.domain.services.dto.command.UpdateNotificationSettingsCommand;
import com.investory.notification.domain.services.dto.result.NotificationSettingsResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationSettingsServiceTest {

    private static final Long USER_ID = 1L;

    @Test
    void 설정_행이_없으면_기본값_전부_수신을_DB에_쓰지_않고_반환한다() {
        FakeNotificationSettingsRepository repository = new FakeNotificationSettingsRepository();
        NotificationSettingsService service = new NotificationSettingsService(repository);

        NotificationSettingsResult result = service.getSettings(USER_ID);

        assertTrue(result.tradeIngestedEnabled());
        assertTrue(result.tendencyAnalyzedEnabled());
        assertTrue(result.simulationCompletedEnabled());
        assertTrue(repository.findByUserId(USER_ID).isEmpty());
    }

    @Test
    void 저장된_설정이_있으면_그대로_반환한다() {
        FakeNotificationSettingsRepository repository = new FakeNotificationSettingsRepository();
        repository.add(NotificationSettings.defaults(USER_ID).update(false, true, false));
        NotificationSettingsService service = new NotificationSettingsService(repository);

        NotificationSettingsResult result = service.getSettings(USER_ID);

        assertEquals(false, result.tradeIngestedEnabled());
        assertEquals(true, result.tendencyAnalyzedEnabled());
        assertEquals(false, result.simulationCompletedEnabled());
    }

    @Test
    void 필드가_하나라도_누락되면_INVALID_SETTINGS_DATA_예외를_던진다() {
        NotificationSettingsService service = new NotificationSettingsService(new FakeNotificationSettingsRepository());

        NotificationException exception = assertThrows(NotificationException.class,
                () -> service.updateSettings(new UpdateNotificationSettingsCommand(USER_ID, true, null, false)));
        assertEquals(NotificationErrorCode.INVALID_SETTINGS_DATA, exception.getErrorCode());
    }

    @Test
    void 행이_없어도_updateSettings를_호출하면_새로_생성된다() {
        FakeNotificationSettingsRepository repository = new FakeNotificationSettingsRepository();
        NotificationSettingsService service = new NotificationSettingsService(repository);

        service.updateSettings(new UpdateNotificationSettingsCommand(USER_ID, false, false, true));

        assertTrue(repository.findByUserId(USER_ID).isPresent());
        assertEquals(false, repository.findByUserId(USER_ID).get().isTradeIngestedEnabled());
    }
}
