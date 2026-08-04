package com.investory.broker.infra.mappers;

import com.investory.broker.infra.entities.BrokerConnectionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface BrokerConnectionMapper {
    List<BrokerConnectionRow> findAllByUserId(@Param("userId") Long userId);

    List<BrokerConnectionRow> findActiveByUserIdAndBrokerId(@Param("userId") Long userId, @Param("brokerId") Long brokerId);

    List<BrokerConnectionRow> findByIdAndUserId(@Param("connectionId") Long connectionId, @Param("userId") Long userId);

    List<BrokerConnectionRow> findByIds(@Param("connectionIds") List<Long> connectionIds);

    List<String> findMockProfileCodeByConnectionId(@Param("connectionId") Long connectionId);

    void insert(BrokerConnectionRow row);

    void updateLastSyncedAt(@Param("connectionId") Long connectionId, @Param("lastSyncedAt") Instant lastSyncedAt);
}