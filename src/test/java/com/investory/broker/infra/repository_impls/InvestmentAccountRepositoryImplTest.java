package com.investory.broker.infra.repository_impls;

import com.investory.broker.infra.entities.InvestmentAccountRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.InvestmentAccountMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvestmentAccountRepositoryImplTest {

    @Test
    void DB_예외는_BrokerInfraException으로_변환된다() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("connection refused");
        InvestmentAccountRepositoryImpl repository = new InvestmentAccountRepositoryImpl(new FailingInvestmentAccountMapper(cause));

        BrokerInfraException exception = assertThrows(BrokerInfraException.class, () -> repository.updateAccountName(25L, "장기 투자용 계좌"));

        assertSame(cause, exception.getCause());
    }

    private static class FailingInvestmentAccountMapper implements InvestmentAccountMapper {
        private final RuntimeException toThrow;

        private FailingInvestmentAccountMapper(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public void upsert(InvestmentAccountRow row) {
            throw toThrow;
        }

        @Override
        public List<InvestmentAccountRow> findByConnectionId(Long connectionId) {
            throw toThrow;
        }

        @Override
        public List<InvestmentAccountRow> findByUserId(Long userId) {
            throw toThrow;
        }

        @Override
        public List<InvestmentAccountRow> findByIds(List<Long> accountIds) {
            throw toThrow;
        }

        @Override
        public List<InvestmentAccountRow> findByIdAndUserId(Long accountId, Long userId) {
            throw toThrow;
        }

        @Override
        public void updateAccountName(Long accountId, String accountName) {
            throw toThrow;
        }
    }
}
