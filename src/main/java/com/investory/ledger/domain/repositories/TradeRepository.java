package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.Trade;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TradeRepository {
    List<Trade> search(TradeSearchCriteria criteria);
    long count(TradeSearchCriteria criteria);
    Optional<Trade> findById(Long tradeId);

    // 적재 시 중복 여부 일괄 판단용 — 넘긴 externalTradeId 중 이미 이 계좌에 존재하는 것만 반환한다.
    Set<String> findExistingExternalTradeIds(Long accountId, List<String> externalTradeIds);

    // FIFO 매칭 재계산용 — tradedAt 오름차순
    List<Trade> findAllByAccountIdAndSecurityId(Long accountId, Long securityId);

    Trade save(Trade trade);

    // 적재 시 대량 저장용 — 거래 건마다 개별 insert하던 것을 한 번에 처리한다.
    void saveAll(List<Trade> trades);

    // 증권사 연동 해지 시 이 계좌의 거래를 지우기 전에, journal에 알릴 tradeId 목록부터 확보한다.
    List<Long> findTradeIdsByAccountId(Long accountId);

    void deleteByAccountId(Long accountId);
}
