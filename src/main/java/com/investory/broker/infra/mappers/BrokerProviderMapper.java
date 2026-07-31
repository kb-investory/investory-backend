package com.investory.broker.infra.mappers;

import com.investory.broker.infra.entities.BrokerProviderRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BrokerProviderMapper {
    List<BrokerProviderRow> findAllActive();
}