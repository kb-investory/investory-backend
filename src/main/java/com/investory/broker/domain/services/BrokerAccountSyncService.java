package com.investory.broker.domain.services;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.ports.BrokerFeedPort;
import com.investory.broker.domain.ports.HoldingIngestionPort;
import com.investory.broker.domain.ports.TradeIngestionPort;
import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawAccountRecord;
import com.investory.broker.domain.ports.dto.RawHoldingBatch;
import com.investory.broker.domain.ports.dto.RawTradeRecord;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 한 번의 sync 시도에서 계좌·거래·보유종목 적재를 전부 하나의 물리 트랜잭션으로 묶어서 실행한다.
// 계좌 하나라도 실패하면 이번 시도에서 이미 처리한 다른 계좌 데이터까지 전부 롤백된다 —
// "계좌 일부만 반영된 애매한 상태"를 남기지 않기 위한 의도적인 전체 원자성이다.
// broker_connections/account_sync_batches 행은 이 트랜잭션 밖(BrokerConnectionService의
// 트랜잭션)에서 관리되므로 sync 실패 여부와 무관하게 그대로 유지된다.
//
// REQUIRES_NEW로 별도 물리 트랜잭션을 만드는 이유: BrokerConnectionService.createConnection()도
// @Transactional인데, 만약 기본 propagation(REQUIRED)으로 여기 합류했다면 이 메서드가 던진 예외를
// BrokerConnectionService가 잡아서 삼키더라도 Spring이 이미 트랜잭션을 rollback-only로 표시해둔 상태라,
// 나중에 커넥션 row를 커밋하려는 시점에 UnexpectedRollbackException이 터진다 — 그러면 커넥션 row까지
// 함께 사라져 "연동은 CONNECTED, sync만 FAILED"라는 기존 정책이 깨진다. REQUIRES_NEW는 이 메서드만의
// 독립된 트랜잭션이라 실패해도 바깥 트랜잭션의 rollback-only 플래그에 영향을 주지 않는다.
//
// BrokerConnectionService가 이 로직을 자기 메서드로 두면 self-invocation이라 프록시를 안 타서
// REQUIRES_NEW가 아예 안 걸리므로, 반드시 별도 빈으로 분리해야 한다.
@Service
public class BrokerAccountSyncService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final TradeIngestionPort tradeIngestionPort;
    private final HoldingIngestionPort holdingIngestionPort;
    private final BrokerFeedPort brokerFeedPort;

    public BrokerAccountSyncService(
            InvestmentAccountRepository investmentAccountRepository,
            TradeIngestionPort tradeIngestionPort,
            HoldingIngestionPort holdingIngestionPort,
            BrokerFeedPort brokerFeedPort) {
        this.investmentAccountRepository = investmentAccountRepository;
        this.tradeIngestionPort = tradeIngestionPort;
        this.holdingIngestionPort = holdingIngestionPort;
        this.brokerFeedPort = brokerFeedPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AccountsSyncOutcome syncAccounts(
            Long userId, Long connectionId, String mockConnectionId, List<RawAccountRecord> accounts) {
        int accountCount = 0;
        int insertedTradeCount = 0;
        int skippedTradeCount = 0;
        int holdingCount = 0;

        for (RawAccountRecord account : accounts) {
            Long accountId = createInvestmentAccount(connectionId, account);
            accountCount++;

            List<RawTradeRecord> trades = brokerFeedPort.fetchTrades(mockConnectionId, account.accountNum());
            IngestResult tradeResult = tradeIngestionPort.ingestTrades(userId, accountId, trades);
            insertedTradeCount += tradeResult.successCount();
            skippedTradeCount += tradeResult.skippedCount();

            RawHoldingBatch holdingBatch = brokerFeedPort.fetchHoldings(mockConnectionId, account.accountNum());
            IngestResult holdingResult = holdingIngestionPort.ingestHoldings(
                    userId, accountId, holdingBatch.baseDate(), holdingBatch.holdings());
            holdingCount += holdingResult.successCount();
        }

        return new AccountsSyncOutcome(accountCount, insertedTradeCount, skippedTradeCount, holdingCount);
    }

    private Long createInvestmentAccount(Long connectionId, RawAccountRecord account) {
        return investmentAccountRepository.upsert(
                connectionId,
                account.accountNum(),
                maskAccountNo(account.accountNum()),
                account.accountName(),
                mapAccountType(account.accountType()),
                account.currencyCode()
        );
    }

    // 마이데이터 account_type 코드 중 "101"(종합위탁계좌)만 문서화되어 있어 일단 전부 STOCK으로 매핑한다.
    private AccountType mapAccountType(String mydataAccountType) {
        return AccountType.STOCK;
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() <= 7) {
            return accountNo;
        }
        String first = accountNo.substring(0, 3);
        String last = accountNo.substring(accountNo.length() - 4);
        return first + "*".repeat(accountNo.length() - 7) + last;
    }

    public record AccountsSyncOutcome(
        int accountCount,
        int insertedTradeCount,
        int skippedTradeCount,
        int holdingCount
    ) {
    }
}
