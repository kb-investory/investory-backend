package com.investory.auth.domain.ports;

// notification.domain.services.NotificationSettingsService.initDefaultSettings(Long)로 위임 예정.
// 회원가입(및 탈퇴 후 재활성화) 시 notification_settings 기본값(전부 수신) 행을 생성한다.
public interface NotificationInitPort {
    void initSettings(Long userId);
}
