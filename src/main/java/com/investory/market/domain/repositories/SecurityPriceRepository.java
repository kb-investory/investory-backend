package com.investory.market.domain.repositories;

import com.investory.market.domain.model.SecurityPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SecurityPriceRepository {
    Optional<SecurityPrice> findBySecurityIdAndPriceDate(Long securityId, LocalDate priceDate);

    // 해당 종목의 가장 최근 날짜 시세 1건 (없으면 empty)
    Optional<SecurityPrice> findLatestBySecurityId(Long securityId);

    // from~to(포함) 기간의 일별 시세 목록, price_date 오름차순
    List<SecurityPrice> findBySecurityIdAndDateRange(Long securityId, LocalDate from, LocalDate to);

    // findBySecurityIdAndDateRange의 배치 버전(#208) — 여러 종목의 시세를 한 번에 조회한다.
    // 결과는 security_id, price_date 오름차순으로 섞여서 온다(호출측이 필요하면 securityId로 묶을 것).
    List<SecurityPrice> findBySecurityIdsAndDateRange(List<Long> securityIds, LocalDate from, LocalDate to);

    SecurityPrice save(SecurityPrice securityPrice);
}
