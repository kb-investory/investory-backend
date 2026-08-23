package com.investory.ledger.domain.services;

import com.investory.ledger.domain.events.TradesIngestedEvent;
import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.ports.MarketDataPort;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.TradeRepository;
import com.investory.ledger.domain.services.dto.command.IngestRawTradesCommand;
import com.investory.ledger.domain.services.dto.command.RawTradeRecord;
import com.investory.ledger.domain.services.dto.result.IngestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// broker의 TradeIngestionPort 구현체가 직접 호출하는 진입점. broker는 원시 데이터만 넘기고,
// 종목 해석·중복 제거는 여기서 처리한다.
//
// 최초 연동은 계좌당 거래가 수백~수천 건 내려오는데, 예전엔 거래마다 중복확인+종목조회+insert로
// DB를 3번씩 왕복했다 — 거래 건수만큼 순차 왕복이 쌓여 느렸다. 지금은 계좌 단위로
// (1) 중복확인 일괄 조회 → (2) 종목코드 일괄 조회 → (3) bulk insert, 총 3번의 DB 호출로 끝낸다.
//
// InnoDB 데드락 재시도(#203): touchedSecurityIds가 여러 개면 tradeMatchingService.rematch()가
// securities FK·인덱스 범위에 걸리는데, 서로 무관한 계좌들이 같은 종목을 동시에 건드리면
// 데드락이 날 수 있다. MySQL은 데드락이 나면 현재 트랜잭션 전체를 롤백시키므로("애플리케이션은
// 재시도할 준비가 되어 있어야 한다" — MySQL 공식 문서), 재시도는 실패한 SQL 한 문장이 아니라
// doIngestTrades() 전체를 매번 새 트랜잭션으로 다시 실행해야 한다. @Transactional 애노테이션
// 대신 JournalService와 같은 TransactionTemplate 방식을 쓰는 이유가 이것이다 — 같은 빈 안에서
// this.doIngestTrades(...)를 반복 호출하는 self-invocation은 AOP 프록시를 타지 않아 재시도할
// 때마다 새 트랜잭션이 열리지 않는다. TransactionTemplate.execute()는 애노테이션 프록시와 같은
// PlatformTransactionManager를 통해 호출할 때마다 새 트랜잭션을 연다.
@Service
public class TradeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TradeIngestionService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MILLIS = 100L;

    private final TradeRepository tradeRepository;
    private final MarketDataPort marketDataPort;
    private final TradeMatchingService tradeMatchingService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public TradeIngestionService(TradeRepository tradeRepository, MarketDataPort marketDataPort,
                                  TradeMatchingService tradeMatchingService, ApplicationEventPublisher eventPublisher,
                                  PlatformTransactionManager transactionManager) {
        this.tradeRepository = tradeRepository;
        this.marketDataPort = marketDataPort;
        this.tradeMatchingService = tradeMatchingService;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public IngestResult ingestTrades(IngestRawTradesCommand command) {
        return retryOnDeadlock(() -> transactionTemplate.execute(status -> doIngestTrades(command)),
                command.accountId());
    }

    // 재시도 메커니즘만 떼어내 독립적으로 테스트할 수 있게 제네릭 헬퍼로 분리했다(package-private).
    // 실제 재실행 단위(work)는 매번 새 트랜잭션을 여는 transactionTemplate.execute() 호출 그 자체다.
    <T> T retryOnDeadlock(Supplier<T> work, Long accountId) {
        int attempt = 1;
        while (true) {
            try {
                return work.get();
            } catch (DeadlockLoserDataAccessException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("거래 적재 중 데드락 발생 — 재시도합니다. accountId={}, attempt={}/{}",
                        accountId, attempt, MAX_ATTEMPTS, e);
                sleep(RETRY_BACKOFF_MILLIS * attempt);
                attempt++;
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("거래 적재 재시도 대기 중 인터럽트됨", ie);
        }
    }

    private IngestResult doIngestTrades(IngestRawTradesCommand command) {
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

        // notification이 구독한다(TradesIngestedEventListener). 전부 중복/스킵이면 새로 적재된
        // 거래가 없으므로 발행하지 않는다.
        if (!tradesToSave.isEmpty()) {
            eventPublisher.publishEvent(new TradesIngestedEvent(command.userId(), command.accountId(), tradesToSave.size()));
        }

        return new IngestResult(tradesToSave.size(), skippedReasons.size(), skippedReasons);
    }
}
