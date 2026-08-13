package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.DailyPriceInfo;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataPort {

    // 특정 종목의 from~to(포함) 기간 일별 종가, 날짜 오름차순. 데이터 없는 날짜는 포함되지 않는다.
    List<DailyPriceInfo> findDailyPrices(Long securityId, LocalDate from, LocalDate to);
}
