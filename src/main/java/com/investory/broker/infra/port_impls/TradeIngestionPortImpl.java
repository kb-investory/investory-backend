package com.investory.broker.infra.port_impls;

import com.investory.broker.domain.ports.TradeIngestionPort;
import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawTradeRecord;
import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.services.TradeIngestionService;
import com.investory.ledger.domain.services.dto.command.IngestRawTradesCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TradeIngestionPortImpl implements TradeIngestionPort {

    private final TradeIngestionService tradeIngestionService;

    public TradeIngestionPortImpl(TradeIngestionService tradeIngestionService) {
        this.tradeIngestionService = tradeIngestionService;
    }

    @Override
    public IngestResult ingestTrades(Long userId, Long accountId, List<RawTradeRecord> rawTrades) {
        List<com.investory.ledger.domain.services.dto.command.RawTradeRecord> ledgerRawTrades = rawTrades.stream()
                .map(this::toLedgerRawTradeRecord)
                .collect(Collectors.toList());

        com.investory.ledger.domain.services.dto.result.IngestResult result = tradeIngestionService.ingestTrades(
                new IngestRawTradesCommand(userId, accountId, ledgerRawTrades));

        return new IngestResult(result.successCount(), result.skippedCount(), result.skippedReasons());
    }

    private com.investory.ledger.domain.services.dto.command.RawTradeRecord toLedgerRawTradeRecord(RawTradeRecord raw) {
        return new com.investory.ledger.domain.services.dto.command.RawTradeRecord(
                raw.externalTradeId(),
                raw.securityCode(),
                TradeSide.valueOf(raw.tradeSide()),
                raw.quantity(),
                raw.unitPrice(),
                raw.transactionCostAmount(),
                raw.tradedAt()
        );
    }
}
