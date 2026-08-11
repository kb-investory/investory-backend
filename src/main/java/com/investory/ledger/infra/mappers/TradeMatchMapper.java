package com.investory.ledger.infra.mappers;

import com.investory.ledger.infra.entities.TradeMatchRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface TradeMatchMapper {

    void deleteByAccountIdAndSecurityId(@Param("accountId") Long accountId, @Param("securityId") Long securityId);

    void insertAll(@Param("rows") List<TradeMatchRow> rows);

    List<Integer> findHoldingDaysByAccountIdsSince(@Param("accountIds") List<Long> accountIds, @Param("since") Instant since);

}
