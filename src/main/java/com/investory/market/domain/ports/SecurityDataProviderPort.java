package com.investory.market.domain.ports;

import com.investory.market.domain.ports.dto.SecurityInfoDto;
import com.investory.market.domain.ports.dto.SecurityPriceDto;

/**
 * 외부 시세 제공자 연동을 추상화하는 포트. 벤더(KIS 등)를 도메인이 알 필요가 없도록
 * 실제 구현(토큰 발급/캐싱, HTTP 호출 등)은 infra/clients 아래에 둔다.
 */
public interface SecurityDataProviderPort {
    // 종목 마스터 정보 조회
    SecurityInfoDto fetchSecurityInfo(String securityCode);

    // 종목 현재가/일별 시세 조회
    SecurityPriceDto fetchDailyPrice(String securityCode);
}
