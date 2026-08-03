package com.investory.ledger.domain.services;

import com.investory.ledger.domain.model.Holding;
import com.investory.ledger.domain.ports.MarketDataPort;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.HoldingSnapshotRepository;
import com.investory.ledger.domain.services.dto.command.IngestRawHoldingsCommand;
import com.investory.ledger.domain.services.dto.command.RawHoldingRecord;
import com.investory.ledger.domain.services.dto.result.IngestResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// broker가 이미 계산해서 넘긴 보유현황을 그대로 저장한다 — trades를 재생해서 검산하지 않는다.
@Service
public class HoldingIngestionService {

    private final HoldingSnapshotRepository holdingSnapshotRepository;
    private final MarketDataPort marketDataPort;

    public HoldingIngestionService(HoldingSnapshotRepository holdingSnapshotRepository, MarketDataPort marketDataPort) {
        this.holdingSnapshotRepository = holdingSnapshotRepository;
        this.marketDataPort = marketDataPort;
    }

    @Transactional
    public IngestResult ingestHoldings(IngestRawHoldingsCommand command) {
        int successCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (RawHoldingRecord raw : command.rawHoldings()) {
            Optional<SecurityInfo> security = marketDataPort.resolveByCode(raw.securityCode());
            if (security.isEmpty()) {
                skippedReasons.add("알 수 없는 종목코드: " + raw.securityCode());
                continue;
            }

            Holding holding = Holding.of(
                    command.accountId(),
                    security.get().securityId(),
                    raw.quantity(),
                    raw.averagePurchasePrice(),
                    raw.currentPrice(),
                    command.baseDate()
            );
            holdingSnapshotRepository.upsert(holding);
            successCount++;
        }

        return new IngestResult(successCount, skippedReasons.size(), skippedReasons);
    }
}
