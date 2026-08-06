package com.investory.market.infra.mappers;

import com.investory.market.infra.entities.StockPriceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface StockPriceMapper {
    StockPriceRow findBySecurityIdAndPriceDate(@Param("securityId") Long securityId,
                                               @Param("priceDate") LocalDate priceDate);

    StockPriceRow findLatestBySecurityId(@Param("securityId") Long securityId);

    void insert(StockPriceRow row);

    void update(StockPriceRow row);
}
