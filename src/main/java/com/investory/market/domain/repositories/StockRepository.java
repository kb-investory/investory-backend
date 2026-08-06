package com.investory.market.domain.repositories;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Stock;

import java.util.List;
import java.util.Optional;

public interface StockRepository {
    Optional<Stock> findByStockCode(String stockCode);

    // 내부 숫자 PK(security_id, 외부 API의 securityId)로 조회
    Optional<Stock> findBySecurityId(Long securityId);

    // securityId 목록 일괄 조회 (다른 도메인의 상세 조회 등에서 N+1 없이 한 번에 가져올 때 사용)
    List<Stock> findBySecurityIds(List<Long> securityIds);

    // 매일 배치가 갱신 대상을 정할 때 사용 - 이미 등록된 종목코드 전체 목록
    List<String> findAllStockCodes();

    // keyword(종목명/종목코드 부분일치)와 marketType(정확일치)로 페이지 단위 검색. 둘 다 null이면 전체 조회.
    List<Stock> search(String keyword, MarketType marketType, int offset, int limit);

    // search와 동일한 조건의 전체 건수 (페이지네이션 totalElements 계산용)
    long countSearch(String keyword, MarketType marketType);

    // security_code 존재 여부로 이미 있으면 갱신, 없으면 신규 저장한다.
    Stock save(Stock stock);
}
