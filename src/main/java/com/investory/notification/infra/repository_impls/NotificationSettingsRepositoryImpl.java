package com.investory.notification.infra.repository_impls;

import com.investory.notification.domain.model.NotificationSettings;
import com.investory.notification.domain.repositories.NotificationSettingsRepository;
import com.investory.notification.infra.entities.NotificationSettingsRow;
import com.investory.notification.infra.exception.NotificationInfraException;
import com.investory.notification.infra.mappers.NotificationSettingsMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class NotificationSettingsRepositoryImpl implements NotificationSettingsRepository {

    private final NotificationSettingsMapper notificationSettingsMapper;

    public NotificationSettingsRepositoryImpl(NotificationSettingsMapper notificationSettingsMapper) {
        this.notificationSettingsMapper = notificationSettingsMapper;
    }

    @Override
    public Optional<NotificationSettings> findByUserId(Long userId) {
        try {
            return notificationSettingsMapper.findByUserId(userId).stream()
                    .map(NotificationSettingsRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림 수신 설정을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void upsert(NotificationSettings settings) {
        NotificationSettingsRow row = NotificationSettingsRow.from(settings);
        try {
            notificationSettingsMapper.upsert(row);
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림 수신 설정을 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteByUserId(Long userId) {
        try {
            notificationSettingsMapper.deleteByUserId(userId);
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림 수신 설정을 삭제하는 중 오류가 발생했습니다.", e);
        }
    }
}
