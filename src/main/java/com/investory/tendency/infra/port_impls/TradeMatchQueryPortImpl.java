package com.investory.tendency.infra.port_impls;

import com.investory.ledger.domain.ports.AccountPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import com.investory.ledger.domain.repositories.TradeMatchRepository;
import com.investory.ledger.infra.exception.LedgerInfraException;
import com.investory.tendency.domain.ports.TradeMatchQueryPort;
import com.investory.tendency.infra.exception.TendencyInfraException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

// ledger 도메인이 공개한 TradeMatchRepository/AccountPort를 통해서만 데이터를 받는다 — ledger의
// 테이블/SQL을 직접 알지 않는다. userId → accountIds 해석은 ledger의 다른 서비스들과 동일하게
// AccountPort로 위임한다.
@Component
public class TradeMatchQueryPortImpl implements TradeMatchQueryPort {

    private static final int ANALYSIS_PERIOD_DAYS = 90;

    private final TradeMatchRepository tradeMatchRepository;
    private final AccountPort accountPort;

    public TradeMatchQueryPortImpl(TradeMatchRepository tradeMatchRepository, AccountPort accountPort) {
        this.tradeMatchRepository = tradeMatchRepository;
        this.accountPort = accountPort;
    }

    @Override
    public List<Integer> findHoldingDaysForLast90Days(Long userId) {
        List<Long> accountIds = accountPort.findAccountsByUserId(userId).stream()
                .map(AccountInfo::accountId)
                .collect(Collectors.toList());
        if (accountIds.isEmpty()) {
            return List.of();
        }

        Instant since = Instant.now().minus(ANALYSIS_PERIOD_DAYS, ChronoUnit.DAYS);
        try {
            return tradeMatchRepository.findHoldingDaysByAccountIdsSince(accountIds, since);
        } catch (LedgerInfraException e) {
            throw new TendencyInfraException("최근 매매 보유기간 데이터를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}
