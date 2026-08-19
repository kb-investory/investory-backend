package com.investory.market.domain.services.dto.command;

// securities/security_daily_prices 두 테이블을 채울 때 쓰는 입력. 종목코드 하나만 있으면 나머지는 KIS API로 채운다.
public record SyncSecurityCommand(String securityCode) {
}
