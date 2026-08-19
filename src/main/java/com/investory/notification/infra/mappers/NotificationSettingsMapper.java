package com.investory.notification.infra.mappers;

import com.investory.notification.infra.entities.NotificationSettingsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationSettingsMapper {
    List<NotificationSettingsRow> findByUserId(@Param("userId") Long userId);

    void upsert(NotificationSettingsRow row);

    void deleteByUserId(@Param("userId") Long userId);
}
