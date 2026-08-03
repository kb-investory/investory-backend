package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawTradeRecord;

import java.util.List;

public class FakeTradeIngestionPort implements TradeIngestionPort {

    private IngestResult nextResult = new IngestResult(0, 0, List.of());

    public void willReturn(IngestResult result) {
        this.nextResult = result;
    }

    @Override
    public IngestResult ingestTrades(Long userId, Long accountId, List<RawTradeRecord> rawTrades) {
        return nextResult;
    }
}
