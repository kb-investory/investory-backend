package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.DailyPriceInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MarketDataPort {

    // 특정 종목의 from~to(포함) 기간 일별 종가, 날짜 오름차순. 데이터 없는 날짜는 포함되지 않는다.
    List<DailyPriceInfo> findDailyPrices(Long securityId, LocalDate from, LocalDate to);

    // findDailyPrices의 배치 버전(#208) — 여러 종목의 시세를 한 번에 조회해 securityId로 묶어 돌려준다.
    // AnalysisRunService.collectLossOrGain()이 종목마다 순차 호출하던 것(사용자당 최대 60회 DB
    // 왕복)을 이걸로 한 번에 처리한다. 요청한 securityId가 결과 맵에 없으면 그 종목은 시세 데이터가
    // 없다는 뜻(빈 리스트가 아니라 키 자체가 없음 — 호출측이 getOrDefault로 처리할 것).
    Map<Long, List<DailyPriceInfo>> findDailyPrices(List<Long> securityIds, LocalDate from, LocalDate to);
}
