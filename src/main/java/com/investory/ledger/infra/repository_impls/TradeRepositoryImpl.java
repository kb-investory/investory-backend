package com.investory.ledger.infra.repository_impls;

import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.repositories.TradeRepository;
import com.investory.ledger.domain.repositories.TradeSearchCriteria;
import com.investory.ledger.infra.entities.TradeRow;
import com.investory.ledger.infra.exception.LedgerInfraException;
import com.investory.ledger.infra.mappers.TradeMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TradeRepositoryImpl implements TradeRepository {

    private final TradeMapper tradeMapper;

    public TradeRepositoryImpl(TradeMapper tradeMapper) {
        this.tradeMapper = tradeMapper;
    }

    @Override
    public List<Trade> search(TradeSearchCriteria criteria) {
        try {
            int offset = criteria.page() * criteria.size();
            return tradeMapper.search(criteria.accountIds(), criteria.securityId(), criteria.tradeSide(),
                            toInclusiveStart(criteria.from()), toExclusiveEnd(criteria.to()), offset, criteria.size())
                    .stream()
                    .map(TradeRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new LedgerInfraException("거래내역을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public long count(TradeSearchCriteria criteria) {
        try {
            return tradeMapper.count(criteria.accountIds(), criteria.securityId(), criteria.tradeSide(),
                    toInclusiveStart(criteria.from()), toExclusiveEnd(criteria.to()));
        } catch (DataAccessException e) {
            throw new LedgerInfraException("거래내역 건수를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<Trade> findById(Long tradeId) {
        try {
            return tradeMapper.findById(tradeId).stream()
                    .map(TradeRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new LedgerInfraException("거래를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<Trade> findByAccountIdAndExternalTradeId(Long accountId, String externalTradeId) {
        try {
            return tradeMapper.findByAccountIdAndExternalTradeId(accountId, externalTradeId).stream()
                    .map(TradeRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new LedgerInfraException("거래 중복 여부를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Trade save(Trade trade) {
        TradeRow row = TradeRow.from(trade);
        try {
            tradeMapper.insert(row);
        } catch (DataAccessException e) {
            throw new LedgerInfraException("거래를 저장하는 중 오류가 발생했습니다.", e);
        }
        return row.toDomain();
    }

    // 시작일 00:00:00(UTC) 포함
    private Instant toInclusiveStart(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    // 종료일 다음날 00:00:00(UTC) 미포함 — traded_at < toExclusive
    private Instant toExclusiveEnd(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
