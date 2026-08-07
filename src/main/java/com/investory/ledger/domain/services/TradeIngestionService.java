package com.investory.ledger.domain.services;

import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.ports.MarketDataPort;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.TradeRepository;
import com.investory.ledger.domain.services.dto.command.IngestRawTradesCommand;
import com.investory.ledger.domain.services.dto.command.RawTradeRecord;
import com.investory.ledger.domain.services.dto.result.IngestResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// broker의 TradeIngestionPort 구현체가 직접 호출하는 진입점. broker는 원시 데이터만 넘기고,
// 종목 해석·중복 제거는 여기서 처리한다.
@Service
public class TradeIngestionService {

    private final TradeRepository tradeRepository;
    private final MarketDataPort marketDataPort;
    private final TradeMatchingService tradeMatchingService;

    public TradeIngestionService(TradeRepository tradeRepository, MarketDataPort marketDataPort,
                                  TradeMatchingService tradeMatchingService) {
        this.tradeRepository = tradeRepository;
        this.marketDataPort = marketDataPort;
        this.tradeMatchingService = tradeMatchingService;
    }

    @Transactional
    public IngestResult ingestTrades(IngestRawTradesCommand command) {
        int successCount = 0;
        List<String> skippedReasons = new ArrayList<>();
        Set<Long> touchedSecurityIds = new HashSet<>();

        for (RawTradeRecord raw : command.rawTrades()) {
            // 이미 적재된 거래는 조용히 건너뛴다 (재동기화에도 안전한 멱등 처리)
            if (tradeRepository.findByAccountIdAndExternalTradeId(command.accountId(), raw.externalTradeId()).isPresent()) {
                continue;
            }

            Optional<SecurityInfo> security = marketDataPort.resolveByCode(raw.securityCode());
            if (security.isEmpty()) {
                skippedReasons.add("알 수 없는 종목코드: " + raw.securityCode());
                continue;
            }

            Trade trade = Trade.create(
                    command.accountId(),
                    security.get().securityId(),
                    raw.tradeSide(),
                    raw.quantity(),
                    raw.unitPrice(),
                    raw.transactionCostAmount(),
                    raw.externalTradeId(),
                    raw.tradedAt()
            );
            tradeRepository.save(trade);
            successCount++;
            touchedSecurityIds.add(security.get().securityId());
        }

        // 새로 적재된 종목에 대해서만 FIFO 매칭을 재계산한다 (전부 중복/스킵이면 재계산 안 함)
        for (Long securityId : touchedSecurityIds) {
            tradeMatchingService.rematch(command.accountId(), securityId);
        }

        return new IngestResult(successCount, skippedReasons.size(), skippedReasons);
    }
}
