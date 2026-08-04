package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.TradeMatch;

import java.util.List;

public interface TradeMatchRepository {

    // 재계산 전 해당 계좌·종목의 기존 매칭을 전부 지운다 (과거 거래 소급 삽입에도 정확성 보장)
    void deleteByAccountIdAndSecurityId(Long accountId, Long securityId);

    void saveAll(List<TradeMatch> matches);
}
