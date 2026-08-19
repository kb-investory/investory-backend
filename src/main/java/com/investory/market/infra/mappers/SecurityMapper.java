package com.investory.market.infra.mappers;

import com.investory.market.infra.entities.SecurityRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecurityMapper {
    SecurityRow findBySecurityCode(@Param("securityCode") String securityCode);

    SecurityRow findBySecurityId(@Param("securityId") Long securityId);

    List<SecurityRow> findBySecurityIds(@Param("securityIds") List<Long> securityIds);

    List<String> findAllSecurityCodes();

    // keyword: 종목명/종목코드 부분일치 검색어 (null이면 전체), marketType: 시장 구분 문자열(KOSPI/KOSDAQ/KONEX, null이면 전체)
    List<SecurityRow> search(@Param("keyword") String keyword,
                          @Param("marketType") String marketType,
                          @Param("offset") int offset,
                          @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword,
                     @Param("marketType") String marketType);

    void insert(SecurityRow row);

    void update(SecurityRow row);
}
