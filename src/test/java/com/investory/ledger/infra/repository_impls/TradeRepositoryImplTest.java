package com.investory.ledger.infra.repository_impls;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.model.TradeFixture;
import com.investory.ledger.domain.repositories.TradeSearchCriteria;
import com.investory.ledger.infra.entities.TradeRow;
import com.investory.ledger.infra.exception.LedgerInfraException;
import com.investory.ledger.infra.mappers.TradeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeRepositoryImplTest {

    @Test
    void search_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeRepositoryImpl repository = new TradeRepositoryImpl(new FailingTradeMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.search(new TradeSearchCriteria(List.of(1L), null, null, null, null, 0, 20)));

        assertSame(cause, exception.getCause());
    }

    @Test
    void count_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeRepositoryImpl repository = new TradeRepositoryImpl(new FailingTradeMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.count(new TradeSearchCriteria(List.of(1L), null, null, null, null, 0, 20)));

        assertSame(cause, exception.getCause());
    }

    @Test
    void findById_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeRepositoryImpl repository = new TradeRepositoryImpl(new FailingTradeMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.findById(501L));

        assertSame(cause, exception.getCause());
    }

    @Test
    void findExistingExternalTradeIds_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeRepositoryImpl repository = new TradeRepositoryImpl(new FailingTradeMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.findExistingExternalTradeIds(1L, List.of("ext-1")));

        assertSame(cause, exception.getCause());
    }

    @Test
    void findAllByAccountIdAndSecurityId_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeRepositoryImpl repository = new TradeRepositoryImpl(new FailingTradeMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.findAllByAccountIdAndSecurityId(1L, 101L));

        assertSame(cause, exception.getCause());
    }

    @Test
    void save_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeRepositoryImpl repository = new TradeRepositoryImpl(new FailingTradeMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.save(TradeFixture.trade(1L, 101L, TradeSide.BUY, "ext-1", Instant.parse("2026-07-29T01:15:00Z"))));

        assertSame(cause, exception.getCause());
    }

    @Test
    void saveAll_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeRepositoryImpl repository = new TradeRepositoryImpl(new FailingTradeMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.saveAll(List.of(
                        TradeFixture.trade(1L, 101L, TradeSide.BUY, "ext-1", Instant.parse("2026-07-29T01:15:00Z")))));

        assertSame(cause, exception.getCause());
    }

    private static class FailingTradeMapper implements TradeMapper {
        private final RuntimeException toThrow;

        private FailingTradeMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public List<TradeRow> search(List<Long> accountIds, Long securityId, TradeSide tradeSide,
                                     Instant fromInclusive, Instant toExclusive, int offset, int size) {
            throw toThrow;
        }

        @Override
        public long count(List<Long> accountIds, Long securityId, TradeSide tradeSide,
                          Instant fromInclusive, Instant toExclusive) {
            throw toThrow;
        }

        @Override
        public List<TradeRow> findById(Long tradeId) {
            throw toThrow;
        }

        @Override
        public List<String> findExistingExternalTradeIds(Long accountId, List<String> externalTradeIds) {
            throw toThrow;
        }

        @Override
        public List<TradeRow> findAllByAccountIdAndSecurityId(Long accountId, Long securityId) {
            throw toThrow;
        }

        @Override
        public void insert(TradeRow row) {
            throw toThrow;
        }

        @Override
        public void insertAll(List<TradeRow> rows) {
            throw toThrow;
        }

        @Override
        public List<Long> findTradeIdsByAccountId(Long accountId) {
            throw toThrow;
        }

        @Override
        public void deleteByAccountId(Long accountId) {
            throw toThrow;
        }
    }
}
