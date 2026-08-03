package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawHoldingRecord;

import java.time.LocalDate;
import java.util.List;

// ledger.domain.services.HoldingIngestionService.ingestHoldings(IngestRawHoldingsCommand)로 위임 예정.
// ledger가 아직 없어 PortImpl은 임시 스텁 상태 — 실제 서비스가 생기면 그쪽을 호출하도록 교체.
public interface HoldingIngestionPort {
    IngestResult ingestHoldings(Long userId, Long accountId, LocalDate baseDate, List<RawHoldingRecord> rawHoldings);
}
