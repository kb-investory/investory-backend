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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TradeRepositoryImpl implements TradeRepository {

    // from/to로 들어오는 LocalDate는 사용자가 보는 화면(투자일지 등)의 날짜 경계이므로,
    // UTC가 아니라 사용자 기준 시간대(KST)의 하루로 해석해서 UTC Instant로 변환한다.
    // journal 도메인뿐 아니라 이 리포지토리를 쓰는 /ledger/trades 자체의 from/to 필터에도 동일하게 적용된다.
    private static final ZoneId JOURNAL_ZONE = ZoneId.of("Asia/Seoul");

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
    public List<Trade> findAllByAccountIdAndSecurityId(Long accountId, Long securityId) {
        try {
            return tradeMapper.findAllByAccountIdAndSecurityId(accountId, securityId).stream()
                    .map(TradeRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new LedgerInfraException("계좌·종목별 거래내역을 조회하는 중 오류가 발생했습니다.", e);
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

    @Override
    public List<Long> findTradeIdsByAccountId(Long accountId) {
        try {
            return tradeMapper.findTradeIdsByAccountId(accountId);
        } catch (DataAccessException e) {
            throw new LedgerInfraException("계좌의 거래 ID 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteByAccountId(Long accountId) {
        try {
            tradeMapper.deleteByAccountId(accountId);
        } catch (DataAccessException e) {
            throw new LedgerInfraException("계좌의 거래를 삭제하는 중 오류가 발생했습니다.", e);
        }
    }

    // 시작일 00:00:00(KST) 포함 — UTC Instant로 변환해서 저장된 traded_at과 비교한다.
    private Instant toInclusiveStart(LocalDate date) {
        return date == null ? null : date.atStartOfDay(JOURNAL_ZONE).toInstant();
    }

    // 종료일 다음날 00:00:00(KST) 미포함 — traded_at < toExclusive
    private Instant toExclusiveEnd(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(JOURNAL_ZONE).toInstant();
    }
}
