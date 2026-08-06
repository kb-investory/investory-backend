package com.investory.market.domain.ports;

import com.investory.market.domain.ports.dto.StockInfoDto;
import com.investory.market.domain.ports.dto.StockPriceDto;

/**
 * 한국투자증권(KIS) Open API 연동을 추상화하는 포트.
 * 실제 구현(토큰 발급/캐싱, HTTP 호출)은 infra/clients/kis 아래에 있다.
 */
public interface KisMarketDataPort {
    // search-stock-info: 종목 마스터 정보 조회
    StockInfoDto fetchStockInfo(String stockCode);

    // inquire-price-2: 종목 현재가/일별 시세 조회
    StockPriceDto fetchDailyPrice(String stockCode);
}
