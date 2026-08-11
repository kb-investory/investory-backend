package com.investory.ledger.infra.repository_impls;

import com.investory.ledger.domain.model.TradeMatch;
import com.investory.ledger.domain.repositories.TradeMatchRepository;
import com.investory.ledger.infra.entities.TradeMatchRow;
import com.investory.ledger.infra.exception.LedgerInfraException;
import com.investory.ledger.infra.mappers.TradeMatchMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TradeMatchRepositoryImpl implements TradeMatchRepository {

    private final TradeMatchMapper tradeMatchMapper;

    public TradeMatchRepositoryImpl(TradeMatchMapper tradeMatchMapper) {
        this.tradeMatchMapper = tradeMatchMapper;
    }

    @Override
    public void deleteByAccountIdAndSecurityId(Long accountId, Long securityId) {
        try {
            tradeMatchMapper.deleteByAccountIdAndSecurityId(accountId, securityId);
        } catch (DataAccessException e) {
            throw new LedgerInfraException("거래 매칭을 삭제하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void saveAll(List<TradeMatch> matches) {
        // 빈 리스트로 INSERT ... VALUES <foreach>를 만들면 SQL 문법 오류가 나므로 매퍼 호출 전에 막는다.
        if (matches.isEmpty()) {
            return;
        }
        List<TradeMatchRow> rows = matches.stream()
                .map(TradeMatchRow::from)
                .collect(Collectors.toList());
        try {
            tradeMatchMapper.insertAll(rows);
        } catch (DataAccessException e) {
            throw new LedgerInfraException("거래 매칭을 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<Integer> findHoldingDaysByAccountIdsSince(List<Long> accountIds, Instant since) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        try {
            return tradeMatchMapper.findHoldingDaysByAccountIdsSince(accountIds, since);
        } catch (DataAccessException e) {
            throw new LedgerInfraException("보유기간 데이터를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}
