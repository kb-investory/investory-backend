package com.investory.notification.infra.repository_impls;

import com.investory.notification.domain.model.Notification;
import com.investory.notification.domain.repositories.NotificationRepository;
import com.investory.notification.infra.entities.NotificationRow;
import com.investory.notification.infra.exception.NotificationInfraException;
import com.investory.notification.infra.mappers.NotificationMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationMapper notificationMapper;

    public NotificationRepositoryImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public List<Notification> findByUser(Long userId, Boolean isRead, int offset, int limit) {
        try {
            return notificationMapper.findByUser(userId, isRead, offset, limit).stream()
                    .map(NotificationRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public long countByUser(Long userId, Boolean isRead) {
        try {
            return notificationMapper.countByUser(userId, isRead);
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림 개수를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<Notification> findById(Long notificationId) {
        try {
            return notificationMapper.findById(notificationId).stream()
                    .map(NotificationRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Notification save(Notification notification) {
        NotificationRow row = NotificationRow.from(notification);
        try {
            notificationMapper.insert(row);
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림을 저장하는 중 오류가 발생했습니다.", e);
        }
        return row.toDomain();
    }

    @Override
    public void update(Notification notification) {
        NotificationRow row = NotificationRow.from(notification);
        try {
            notificationMapper.update(row);
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림을 수정하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public int markAllAsRead(Long userId, Instant readAt) {
        try {
            return notificationMapper.markAllAsRead(userId, readAt);
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림을 일괄 읽음처리하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteByUserId(Long userId) {
        try {
            notificationMapper.deleteByUserId(userId);
        } catch (DataAccessException e) {
            throw new NotificationInfraException("알림을 삭제하는 중 오류가 발생했습니다.", e);
        }
    }
}
