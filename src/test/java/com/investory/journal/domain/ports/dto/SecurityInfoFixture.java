package com.investory.journal.domain.ports.dto;

import com.investory.journal.domain.constant.MarketType;

public class SecurityInfoFixture {

    public static SecurityInfo samsungElectronics(Long securityId) {
        return new SecurityInfo(securityId, "005930", "삼성전자", MarketType.KOSPI);
    }

    public static SecurityInfo skHynix(Long securityId) {
        return new SecurityInfo(securityId, "000660", "SK하이닉스", MarketType.KOSPI);
    }
}
