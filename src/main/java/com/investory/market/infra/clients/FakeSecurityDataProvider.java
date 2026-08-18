package com.investory.market.infra.clients;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.ports.SecurityDataProviderPort;
import com.investory.market.domain.ports.dto.SecurityInfoDto;
import com.investory.market.domain.ports.dto.SecurityPriceDto;
import com.investory.market.infra.clients.kis.KisDisabledCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

// 개발/로컬용 — 실제 KIS 호출 없이 종목코드 기반의 결정론적인 가짜 정보/시세를 반환한다.
// kis.enabled=false일 때만 활성화된다.
@Component
@Conditional(KisDisabledCondition.class)
public class FakeSecurityDataProvider implements SecurityDataProviderPort {

    @Override
    public SecurityInfoDto fetchSecurityInfo(String securityCode) {
        int seed = Math.abs(securityCode.hashCode());

        return SecurityInfoDto.builder()
                .securityCode(securityCode)
                .securityName("테스트종목-" + securityCode)
                .marketType(seed % 2 == 0 ? MarketType.KOSPI : MarketType.KOSDAQ)
                .sectorCode("999")
                .sectorName("테스트업종")
                .stdIdstClsfName("테스트표준산업분류")
                .listedDate(LocalDate.of(2010, 1, 4))
                .delistedDate(null)
                .isActive(true)
                .build();
    }

    @Override
    public SecurityPriceDto fetchDailyPrice(String securityCode) {
        int seed = Math.abs(securityCode.hashCode());
        long basePrice = 10_000 + (seed % 90_000);
        long fluctuation = 1 + (seed % 500);

        return SecurityPriceDto.builder()
                .priceDate(LocalDate.now())
                .lowPrice(basePrice - fluctuation)
                .highPrice(basePrice + fluctuation)
                .openPrice(basePrice - fluctuation / 2)
                .closePrice(basePrice)
                .dailyReturnRate(BigDecimal.valueOf((seed % 21) - 10, 1))
                .tradingVolume((long) (seed % 1_000_000) + 1000)
                .tradingValue(basePrice * ((seed % 1_000_000) + 1000))
                .build();
    }
}
