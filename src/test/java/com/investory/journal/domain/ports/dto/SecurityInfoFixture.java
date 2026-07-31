package com.investory.journal.domain.ports.dto;

public class SecurityInfoFixture {

    public static SecurityInfo samsungElectronics(Long securityId) {
        return new SecurityInfo(securityId, "005930", "삼성전자");
    }

    public static SecurityInfo skHynix(Long securityId) {
        return new SecurityInfo(securityId, "000660", "SK하이닉스");
    }
}
