package com.investory.broker.infra.port_impls;

import com.investory.broker.domain.ports.HoldingIngestionPort;
import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawHoldingRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
public class HoldingIngestionPortImpl implements HoldingIngestionPort {

    // TODO: ledger.domain.services.HoldingIngestionService 구현 후 실제 호출로 교체.
    // ledger가 아직 없어 전달받은 보유종목을 전부 스킵 처리하고 반환한다.
    @Override
    public IngestResult ingestHoldings(Long userId, Long accountId, LocalDate baseDate, List<RawHoldingRecord> rawHoldings) {
        return new IngestResult(0, rawHoldings.size(), Collections.nCopies(rawHoldings.size(), "ledger 미구현"));
    }
}
