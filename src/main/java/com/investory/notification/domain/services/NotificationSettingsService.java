package com.investory.notification.domain.services;

import com.investory.notification.domain.exception.NotificationErrorCode;
import com.investory.notification.domain.exception.NotificationException;
import com.investory.notification.domain.model.NotificationSettings;
import com.investory.notification.domain.repositories.NotificationSettingsRepository;
import com.investory.notification.domain.services.dto.command.UpdateNotificationSettingsCommand;
import com.investory.notification.domain.services.dto.result.NotificationSettingsResult;
import org.springframework.stereotype.Service;

@Service
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;

    public NotificationSettingsService(NotificationSettingsRepository notificationSettingsRepository) {
        this.notificationSettingsRepository = notificationSettingsRepository;
    }

    // 설정 행이 없으면(가입 초기화 연동 전이거나 아직 저장한 적 없는 사용자) DB에 쓰지 않고
    // 기본값(전부 수신)만 보여준다 — auth의 NotificationInitPort 연동(별도 작업) 이전에도 안전하게 동작한다.
    public NotificationSettingsResult getSettings(Long userId) {
        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> NotificationSettings.defaults(userId));
        return NotificationSettingsResult.from(settings);
    }

    // auth.domain.ports.NotificationInitPort 구현체에서만 호출 — 회원가입(및 탈퇴 후 재활성화) 시
    // notification_settings 기본값(전부 수신) 행을 만든다. 이미 행이 있으면 건드리지 않는다(멱등).
    public void initDefaultSettings(Long userId) {
        if (notificationSettingsRepository.findByUserId(userId).isPresent()) {
            return;
        }
        notificationSettingsRepository.upsert(NotificationSettings.defaults(userId));
    }

    // 전체 교체 API라 3개 필드 모두 필수다. 행이 없으면 새로 만든다(upsert).
    public NotificationSettingsResult updateSettings(UpdateNotificationSettingsCommand command) {
        if (command.tradeIngestedEnabled() == null || command.tendencyAnalyzedEnabled() == null
                || command.simulationCompletedEnabled() == null) {
            throw new NotificationException(NotificationErrorCode.INVALID_SETTINGS_DATA);
        }

        NotificationSettings current = notificationSettingsRepository.findByUserId(command.userId())
                .orElseGet(() -> NotificationSettings.defaults(command.userId()));
        NotificationSettings updated = current.update(
                command.tradeIngestedEnabled(), command.tendencyAnalyzedEnabled(), command.simulationCompletedEnabled());
        notificationSettingsRepository.upsert(updated);
        return NotificationSettingsResult.from(updated);
    }
}
