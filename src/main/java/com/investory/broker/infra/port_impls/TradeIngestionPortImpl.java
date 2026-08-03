package com.investory.broker.infra.port_impls;

import com.investory.broker.domain.ports.TradeIngestionPort;
import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawTradeRecord;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TradeIngestionPortImpl implements TradeIngestionPort {

    // TODO: ledger.domain.services.TradeIngestionService 구현 후 실제 호출로 교체.
    // ledger가 아직 없어 전달받은 거래를 전부 스킵 처리하고 반환한다.
    @Override
    public IngestResult ingestTrades(Long userId, Long accountId, List<RawTradeRecord> rawTrades) {
        return new IngestResult(0, rawTrades.size(), Collections.nCopies(rawTrades.size(), "ledger 미구현"));
    }
}
