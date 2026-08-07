package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.Trade;

import java.util.List;
import java.util.Optional;

public interface TradeRepository {
    List<Trade> search(TradeSearchCriteria criteria);
    long count(TradeSearchCriteria criteria);
    Optional<Trade> findById(Long tradeId);

    // 적재 시 중복 여부 판단용
    Optional<Trade> findByAccountIdAndExternalTradeId(Long accountId, String externalTradeId);

    // FIFO 매칭 재계산용 — tradedAt 오름차순
    List<Trade> findAllByAccountIdAndSecurityId(Long accountId, Long securityId);

    Trade save(Trade trade);
}
