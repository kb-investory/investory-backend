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
import java.util.Map;
import java.util.stream.Collectors;

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
        List<RawHoldingRecord> rawHoldings = command.rawHoldings();
        if (rawHoldings.isEmpty()) {
            return new IngestResult(0, 0, List.of());
        }

        // 종목코드를 건별로 조회하지 않고 한 번에 일괄 조회한다 (보유종목 개수만큼 DB 왕복하던 것 제거)
        List<String> securityCodes = rawHoldings.stream().map(RawHoldingRecord::securityCode).distinct().collect(Collectors.toList());
        Map<String, SecurityInfo> securitiesByCode = marketDataPort.resolveByCodes(securityCodes);

        int successCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (RawHoldingRecord raw : rawHoldings) {
            SecurityInfo security = securitiesByCode.get(raw.securityCode());
            if (security == null) {
                skippedReasons.add("알 수 없는 종목코드: " + raw.securityCode());
                continue;
            }

            Holding holding = Holding.of(
                    command.accountId(),
                    security.securityId(),
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
