package com.investory.ledger.infra.repository_impls;

import com.investory.ledger.domain.model.TradeMatch;
import com.investory.ledger.infra.entities.TradeMatchRow;
import com.investory.ledger.infra.exception.LedgerInfraException;
import com.investory.ledger.infra.mappers.TradeMatchMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeMatchRepositoryImplTest {

    @Test
    void deleteByAccountIdAndSecurityId_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeMatchRepositoryImpl repository = new TradeMatchRepositoryImpl(new FailingTradeMatchMapper(cause));

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.deleteByAccountIdAndSecurityId(1L, 101L));

        assertSame(cause, exception.getCause());
    }

    @Test
    void saveAll_DB_예외는_LedgerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        TradeMatchRepositoryImpl repository = new TradeMatchRepositoryImpl(new FailingTradeMatchMapper(cause));

        TradeMatch match = TradeMatch.of(1L, 2L, 101L, BigDecimal.TEN,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1200), BigDecimal.valueOf(2000), BigDecimal.valueOf(20), 10L);

        LedgerInfraException exception = assertThrows(LedgerInfraException.class,
                () -> repository.saveAll(List.of(match)));

        assertSame(cause, exception.getCause());
    }

    @Test
    void saveAll_매칭이_비어있으면_매퍼를_호출하지_않는다() {
        TradeMatchRepositoryImpl repository = new TradeMatchRepositoryImpl(
                new FailingTradeMatchMapper(new RuntimeException("호출되면 안 됨")));

        // 매퍼가 호출됐다면 FailingTradeMatchMapper가 예외를 던져서 이 테스트가 실패한다
        repository.saveAll(List.of());
    }

    private static class FailingTradeMatchMapper implements TradeMatchMapper {
        private final RuntimeException toThrow;

        private FailingTradeMatchMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public void deleteByAccountIdAndSecurityId(Long accountId, Long securityId) {
            throw toThrow;
        }

        @Override
        public void deleteByAccountId(Long accountId) {
            throw toThrow;
        }

        @Override
        public void insertAll(List<TradeMatchRow> rows) {
            throw toThrow;
        }

        @Override
        public List<Integer> findHoldingDaysByAccountIdsSince(List<Long> accountIds, Instant since) {
            return List.of();
        }
    }
}
