package com.investory.market.infra.mappers;

import com.investory.market.infra.entities.StockRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockMapper {
    StockRow findByStockCode(@Param("stockCode") String stockCode);

    StockRow findBySecurityId(@Param("securityId") Long securityId);

    List<String> findAllStockCodes();

    // keyword: 종목명/종목코드 부분일치 검색어 (null이면 전체), marketType: 시장 구분 문자열(KOSPI/KOSDAQ/KONEX, null이면 전체)
    List<StockRow> search(@Param("keyword") String keyword,
                           @Param("marketType") String marketType,
                           @Param("offset") int offset,
                           @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword,
                      @Param("marketType") String marketType);

    void insert(StockRow row);

    void update(StockRow row);
}
