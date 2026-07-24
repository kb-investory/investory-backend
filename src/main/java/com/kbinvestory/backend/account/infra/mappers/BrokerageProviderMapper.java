package com.kbinvestory.backend.account.infra.mappers;

import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;
import com.kbinvestory.backend.account.infra.entities.BrokerageProviderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BrokerageProviderMapper {
    List<BrokerageProviderRow> search(GetBrokersQuery query);
    BrokerageProviderRow findById(@Param("providerId") Long providerId);
}