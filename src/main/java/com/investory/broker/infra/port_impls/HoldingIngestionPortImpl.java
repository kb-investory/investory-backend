package com.investory.broker.infra.port_impls;

import com.investory.broker.domain.ports.HoldingIngestionPort;
import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawHoldingRecord;
import com.investory.ledger.domain.services.HoldingIngestionService;
import com.investory.ledger.domain.services.dto.command.IngestRawHoldingsCommand;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HoldingIngestionPortImpl implements HoldingIngestionPort {

    private final HoldingIngestionService holdingIngestionService;

    public HoldingIngestionPortImpl(HoldingIngestionService holdingIngestionService) {
        this.holdingIngestionService = holdingIngestionService;
    }

    @Override
    public IngestResult ingestHoldings(Long userId, Long accountId, LocalDate baseDate, List<RawHoldingRecord> rawHoldings) {
        List<com.investory.ledger.domain.services.dto.command.RawHoldingRecord> ledgerRawHoldings = rawHoldings.stream()
                .map(this::toLedgerRawHoldingRecord)
                .collect(Collectors.toList());

        com.investory.ledger.domain.services.dto.result.IngestResult result = holdingIngestionService.ingestHoldings(
                new IngestRawHoldingsCommand(userId, accountId, baseDate, ledgerRawHoldings));

        return new IngestResult(result.successCount(), result.skippedCount(), result.skippedReasons());
    }

    private com.investory.ledger.domain.services.dto.command.RawHoldingRecord toLedgerRawHoldingRecord(RawHoldingRecord raw) {
        return new com.investory.ledger.domain.services.dto.command.RawHoldingRecord(
                raw.securityCode(),
                raw.quantity(),
                raw.averagePurchasePrice(),
                raw.currentPrice()
        );
    }
}
