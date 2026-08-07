package com.investory.market.infra.mappers;

import com.investory.market.infra.entities.StockPriceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StockPriceMapper {
    StockPriceRow findBySecurityIdAndPriceDate(@Param("securityId") Long securityId,
                                               @Param("priceDate") LocalDate priceDate);

    StockPriceRow findLatestBySecurityId(@Param("securityId") Long securityId);

    List<StockPriceRow> findBySecurityIdAndDateRange(@Param("securityId") Long securityId,
                                                       @Param("from") LocalDate from,
                                                       @Param("to") LocalDate to);

    void insert(StockPriceRow row);

    void update(StockPriceRow row);
}
