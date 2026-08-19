package com.investory.notification.infra.mappers;

import com.investory.notification.infra.entities.NotificationRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    List<NotificationRow> findByUser(@Param("userId") Long userId, @Param("isRead") Boolean isRead,
                                      @Param("offset") int offset, @Param("limit") int limit);

    long countByUser(@Param("userId") Long userId, @Param("isRead") Boolean isRead);

    List<NotificationRow> findById(@Param("notificationId") Long notificationId);

    void insert(NotificationRow row);

    void update(NotificationRow row);

    void deleteByUserId(@Param("userId") Long userId);
}
