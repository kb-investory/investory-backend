package com.investory.market.infra.mappers;

import com.investory.market.infra.entities.SecurityPriceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SecurityPriceMapper {
    SecurityPriceRow findBySecurityIdAndPriceDate(@Param("securityId") Long securityId,
                                               @Param("priceDate") LocalDate priceDate);

    SecurityPriceRow findLatestBySecurityId(@Param("securityId") Long securityId);

    List<SecurityPriceRow> findBySecurityIdAndDateRange(@Param("securityId") Long securityId,
                                                       @Param("from") LocalDate from,
                                                       @Param("to") LocalDate to);

    void insert(SecurityPriceRow row);

    void update(SecurityPriceRow row);
}
