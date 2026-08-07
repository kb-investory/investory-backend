package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawHoldingRecord;

import java.time.LocalDate;
import java.util.List;

public class FakeHoldingIngestionPort implements HoldingIngestionPort {

    private IngestResult nextResult = new IngestResult(0, 0, List.of());

    public void willReturn(IngestResult result) {
        this.nextResult = result;
    }

    @Override
    public IngestResult ingestHoldings(Long userId, Long accountId, LocalDate baseDate, List<RawHoldingRecord> rawHoldings) {
        return nextResult;
    }
}
