package com.investory.market.domain.services.dto.query;


import com.investory.market.domain.constant.MarketType;

// GET /market/securities 목록 조회 요청 파라미터. keyword/marketType은 선택값(null 허용).
public record SecuritySearchQuery(String keyword, MarketType marketType, int page, int size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    // 컨트롤러에서 받은 원시 쿼리 파라미터(page/size가 없으면 기본값, marketType 문자열은 검증 후 enum으로 변환)를
    // 이 record로 정규화할 때 사용한다. 유효하지 않은 marketType 문자열이면 IllegalArgumentException을 던진다.
    public static SecuritySearchQuery of(String keyword, String marketTypeStr, Integer page, Integer size) {
        MarketType marketType = null;
        if (marketTypeStr != null && !marketTypeStr.isBlank()) {
            marketType = MarketType.valueOf(marketTypeStr.trim().toUpperCase());
        }
        int resolvedPage = page != null ? page : DEFAULT_PAGE;
        int resolvedSize = size != null ? size : DEFAULT_SIZE;
        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        return new SecuritySearchQuery(normalizedKeyword, marketType, resolvedPage, resolvedSize);
    }
}
