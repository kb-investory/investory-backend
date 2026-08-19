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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// broker의 TradeIngestionPort 구현체가 직접 호출하는 진입점. broker는 원시 데이터만 넘기고,
// 종목 해석·중복 제거는 여기서 처리한다.
//
// 최초 연동은 계좌당 거래가 수백~수천 건 내려오는데, 예전엔 거래마다 중복확인+종목조회+insert로
// DB를 3번씩 왕복했다 — 거래 건수만큼 순차 왕복이 쌓여 느렸다. 지금은 계좌 단위로
// (1) 중복확인 일괄 조회 → (2) 종목코드 일괄 조회 → (3) bulk insert, 총 3번의 DB 호출로 끝낸다.
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
        List<RawTradeRecord> rawTrades = command.rawTrades();
        if (rawTrades.isEmpty()) {
            return new IngestResult(0, 0, List.of());
        }

        // 이미 적재된 거래는 조용히 건너뛴다 (재동기화에도 안전한 멱등 처리)
        List<String> externalTradeIds = rawTrades.stream().map(RawTradeRecord::externalTradeId).collect(Collectors.toList());
        Set<String> existing = tradeRepository.findExistingExternalTradeIds(command.accountId(), externalTradeIds);
        List<RawTradeRecord> newTrades = rawTrades.stream()
                .filter(raw -> !existing.contains(raw.externalTradeId()))
                .collect(Collectors.toList());

        List<String> securityCodes = newTrades.stream().map(RawTradeRecord::securityCode).distinct().collect(Collectors.toList());
        Map<String, SecurityInfo> securitiesByCode = marketDataPort.resolveByCodes(securityCodes);

        List<String> skippedReasons = new ArrayList<>();
        List<Trade> tradesToSave = new ArrayList<>();
        Set<Long> touchedSecurityIds = new HashSet<>();

        for (RawTradeRecord raw : newTrades) {
            SecurityInfo security = securitiesByCode.get(raw.securityCode());
            if (security == null) {
                skippedReasons.add("알 수 없는 종목코드: " + raw.securityCode());
                continue;
            }

            tradesToSave.add(Trade.create(
                    command.accountId(),
                    security.securityId(),
                    raw.tradeSide(),
                    raw.quantity(),
                    raw.unitPrice(),
                    raw.transactionCostAmount(),
                    raw.externalTradeId(),
                    raw.tradedAt()
            ));
            touchedSecurityIds.add(security.securityId());
        }

        tradeRepository.saveAll(tradesToSave);

        // 새로 적재된 종목에 대해서만 FIFO 매칭을 재계산한다 (전부 중복/스킵이면 재계산 안 함)
        for (Long securityId : touchedSecurityIds) {
            tradeMatchingService.rematch(command.accountId(), securityId);
        }

        return new IngestResult(tradesToSave.size(), skippedReasons.size(), skippedReasons);
    }
}
