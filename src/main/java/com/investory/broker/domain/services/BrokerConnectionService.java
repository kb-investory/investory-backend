package com.investory.broker.domain.services;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.exception.BrokerErrorCode;
import com.investory.broker.domain.exception.BrokerException;
import com.investory.broker.domain.model.AccountSyncBatch;
import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.model.BrokerProvider;
import com.investory.broker.domain.ports.BrokerFeedPort;
import com.investory.broker.domain.ports.HoldingIngestionPort;
import com.investory.broker.domain.ports.TradeIngestionPort;
import com.investory.broker.domain.ports.dto.BrokerLoginResult;
import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawAccountRecord;
import com.investory.broker.domain.ports.dto.RawHoldingBatch;
import com.investory.broker.domain.ports.dto.RawTradeRecord;
import com.investory.broker.domain.repositories.AccountSyncBatchRepository;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.repositories.BrokerProviderRepository;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.domain.services.dto.command.CreateBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.query.GetBrokerConnectionDetailQuery;
import com.investory.broker.domain.services.dto.result.BrokerConnectionDetailResult;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.CreateBrokerConnectionResult;
import com.investory.broker.infra.exception.BrokerFeedAuthFailedException;
import com.investory.core.exception.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrokerConnectionService {

    private static final Logger log = LoggerFactory.getLogger(BrokerConnectionService.class);
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final BrokerConnectionRepository brokerConnectionRepository;
    private final BrokerProviderRepository brokerProviderRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final AccountSyncBatchRepository accountSyncBatchRepository;
    private final TradeIngestionPort tradeIngestionPort;
    private final HoldingIngestionPort holdingIngestionPort;
    private final BrokerFeedPort brokerFeedPort;

    public BrokerConnectionService(
            BrokerConnectionRepository brokerConnectionRepository,
            BrokerProviderRepository brokerProviderRepository,
            InvestmentAccountRepository investmentAccountRepository,
            AccountSyncBatchRepository accountSyncBatchRepository,
            TradeIngestionPort tradeIngestionPort,
            HoldingIngestionPort holdingIngestionPort,
            BrokerFeedPort brokerFeedPort) {
        this.brokerConnectionRepository = brokerConnectionRepository;
        this.brokerProviderRepository = brokerProviderRepository;
        this.investmentAccountRepository = investmentAccountRepository;
        this.accountSyncBatchRepository = accountSyncBatchRepository;
        this.tradeIngestionPort = tradeIngestionPort;
        this.holdingIngestionPort = holdingIngestionPort;
        this.brokerFeedPort = brokerFeedPort;
    }

    public List<BrokerConnectionResult> getConnections(Long userId) {
        return brokerConnectionRepository.findAllByUserId(userId).stream()
                .map(BrokerConnectionResult::from)
                .collect(Collectors.toList());
    }

    public BrokerConnectionDetailResult getConnectionDetail(GetBrokerConnectionDetailQuery query) {
        BrokerConnection connection = brokerConnectionRepository
                .findByIdAndUserId(query.connectionId(), query.userId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.CONNECTION_NOT_FOUND));

        AccountSyncBatch latestSyncBatch = accountSyncBatchRepository
                .findLatestByConnectionId(connection.getConnectionId())
                .orElse(null);

        return BrokerConnectionDetailResult.of(connection, latestSyncBatch);
    }

    @Transactional
    public CreateBrokerConnectionResult createConnection(CreateBrokerConnectionCommand command) {
        validate(command);

        BrokerProvider provider = brokerProviderRepository.findById(command.brokerId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.PROVIDER_NOT_FOUND));

        if (brokerConnectionRepository.findActiveByUserIdAndBrokerId(command.userId(), command.brokerId()).isPresent()) {
            throw new BrokerException(BrokerErrorCode.ALREADY_CONNECTED);
        }

        BrokerLoginResult login = authenticate(command.loginId(), command.password());

        Instant connectedAt = Instant.now();
        Long connectionId = brokerConnectionRepository.insert(command.userId(), command.brokerId(), command.loginId(), connectedAt);
        Long syncBatchId = accountSyncBatchRepository.create(connectionId);

        SyncOutcome outcome = runSync(command.userId(), connectionId, login.accessToken(), provider.getBrokerCode());

        Instant lastSyncedAt = null;
        SyncStatus syncStatus;
        if (outcome.succeeded()) {
            accountSyncBatchRepository.markSuccess(syncBatchId);
            lastSyncedAt = Instant.now();
            brokerConnectionRepository.updateLastSyncedAt(connectionId, lastSyncedAt);
            syncStatus = SyncStatus.SUCCESS;
        } else {
            accountSyncBatchRepository.markFailed(syncBatchId, outcome.errorMessage());
            syncStatus = SyncStatus.FAILED;
        }

        CreateBrokerConnectionResult.SyncResult syncResult = new CreateBrokerConnectionResult.SyncResult(
                syncBatchId, syncStatus, outcome.accountCount(), outcome.insertedTradeCount(), outcome.holdingCount());

        return new CreateBrokerConnectionResult(
                connectionId,
                provider.getBrokerId(),
                provider.getBrokerCode(),
                provider.getBrokerName(),
                ConnectionStatus.CONNECTED,
                connectedAt,
                lastSyncedAt,
                syncResult
        );
    }

    private void validate(CreateBrokerConnectionCommand command) {
        List<FieldError> errors = new ArrayList<>();
        if (command.brokerId() == null || command.brokerId() < 1) {
            errors.add(new FieldError("brokerId", "brokerId는 1 이상이어야 합니다."));
        }
        if (!StringUtils.hasText(command.loginId()) || command.loginId().length() > 100) {
            errors.add(new FieldError("loginId", "loginId는 1~100자여야 합니다."));
        }
        if (!StringUtils.hasText(command.password()) || command.password().length() > 100) {
            errors.add(new FieldError("password", "password는 1~100자여야 합니다."));
        }
        if (!errors.isEmpty()) {
            throw new BrokerException(BrokerErrorCode.INVALID_CONNECTION_DATA, errors);
        }
    }

    private BrokerLoginResult authenticate(String loginId, String password) {
        try {
            return brokerFeedPort.login(loginId, password);
        } catch (BrokerFeedAuthFailedException e) {
            throw new BrokerException(BrokerErrorCode.BROKER_AUTH_FAILED);
        }
    }

    // 계좌 목록부터 계좌별 거래내역/보유상품까지 조회해서 investment_accounts에 저장하고,
    // 거래/보유종목은 ledger 쪽 Port로 저장 요청만 넘긴다. 도중에 실패해도 커넥션 자체는 롤백하지 않고
    // 실패 사실만 호출자에게 돌려준다 (배치 상태를 FAILED로 남기기 위함).
    private SyncOutcome runSync(Long userId, Long connectionId, String accessToken, String orgCode) {
        try {
            List<RawAccountRecord> accounts = brokerFeedPort.fetchAccounts(accessToken, orgCode);

            int accountCount = 0;
            int insertedTradeCount = 0;
            int holdingCount = 0;

            for (RawAccountRecord account : accounts) {
                Long accountId = createInvestmentAccount(connectionId, account);
                accountCount++;

                List<RawTradeRecord> trades = brokerFeedPort.fetchTrades(accessToken, account.accountNum());
                IngestResult tradeResult = tradeIngestionPort.ingestTrades(userId, accountId, trades);
                insertedTradeCount += tradeResult.successCount();

                RawHoldingBatch holdingBatch = brokerFeedPort.fetchHoldings(accessToken, account.accountNum());
                IngestResult holdingResult = holdingIngestionPort.ingestHoldings(
                        userId, accountId, holdingBatch.baseDate(), holdingBatch.holdings());
                holdingCount += holdingResult.successCount();
            }

            return new SyncOutcome(true, null, accountCount, insertedTradeCount, holdingCount);
        } catch (Exception e) {
            log.error("증권사 동기화 중 오류가 발생했습니다. connectionId={}", connectionId, e);
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new SyncOutcome(false, truncate(message, ERROR_MESSAGE_MAX_LENGTH), 0, 0, 0);
        }
    }

    private Long createInvestmentAccount(Long connectionId, RawAccountRecord account) {
        return investmentAccountRepository.insert(
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

    private String truncate(String message, int maxLength) {
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }

    private record SyncOutcome(
        boolean succeeded,
        String errorMessage,
        int accountCount,
        int insertedTradeCount,
        int holdingCount
    ) {
    }
}
