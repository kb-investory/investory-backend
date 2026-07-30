package com.investory.journal.infra.port_impls;

import com.investory.journal.domain.ports.TradeLedgerPort;
import com.investory.journal.domain.ports.dto.TradeCountInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TradeLedgerPortImpl implements TradeLedgerPort {

    // TODO: ledger.trades 구현 후 실제 집계 호출로 교체. ledger가 아직 없어 항상 빈 값(tradeCount=0)을 반환한다.
    @Override
    public List<TradeCountInfo> countTradesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }
}
