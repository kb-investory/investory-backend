package com.investory.ledger.infra.mappers;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.infra.entities.TradeRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface TradeMapper {

    List<TradeRow> search(@Param("accountIds") List<Long> accountIds,
                           @Param("securityId") Long securityId,
                           @Param("tradeSide") TradeSide tradeSide,
                           @Param("fromInclusive") Instant fromInclusive,
                           @Param("toExclusive") Instant toExclusive,
                           @Param("offset") int offset,
                           @Param("size") int size);

    long count(@Param("accountIds") List<Long> accountIds,
               @Param("securityId") Long securityId,
               @Param("tradeSide") TradeSide tradeSide,
               @Param("fromInclusive") Instant fromInclusive,
               @Param("toExclusive") Instant toExclusive);

    List<TradeRow> findById(@Param("tradeId") Long tradeId);

    List<TradeRow> findByAccountIdAndExternalTradeId(@Param("accountId") Long accountId,
                                                       @Param("externalTradeId") String externalTradeId);

    void insert(TradeRow row);
}
