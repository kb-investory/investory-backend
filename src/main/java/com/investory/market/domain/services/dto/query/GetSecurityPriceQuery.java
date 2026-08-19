package com.investory.market.domain.services.dto.query;

import java.time.LocalDate;

// 프론트에서 특정 종목의 특정 날짜 시세를 조회할 때 쓰는 입력
public record GetSecurityPriceQuery(String securityCode, LocalDate date) {
}
