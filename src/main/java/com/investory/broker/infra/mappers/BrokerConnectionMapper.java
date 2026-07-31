package com.investory.broker.infra.mappers;

import com.investory.broker.infra.entities.BrokerConnectionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BrokerConnectionMapper {
    List<BrokerConnectionRow> findAllByUserId(@Param("userId") Long userId);
}