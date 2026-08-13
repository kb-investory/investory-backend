package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.HoldingWeightInfo;

import java.util.List;

public interface HoldingSummaryPort {

    // 유저의 현재(최신 스냅샷) 보유 종목별 포트폴리오 비중(%). 보유 종목이 없으면 빈 리스트.
    List<HoldingWeightInfo> findHoldingWeights(Long userId);
}
