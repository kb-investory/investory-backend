package com.investory.broker.domain.services;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.exception.BrokerErrorCode;
import com.investory.broker.domain.exception.BrokerException;
import com.investory.broker.domain.model.AccountSyncBatch;
import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.model.BrokerProvider;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.ports.AccountDataCleanupPort;
import com.investory.broker.domain.ports.BrokerFeedPort;
import com.investory.broker.domain.ports.dto.BrokerLoginResult;
import com.investory.broker.domain.ports.dto.RawAccountRecord;
import com.investory.broker.domain.repositories.AccountSyncBatchRepository;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.repositories.BrokerProviderRepository;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.domain.services.dto.command.CreateBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.command.DisconnectBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.command.SyncBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.query.GetBrokerConnectionDetailQuery;
import com.investory.broker.domain.services.dto.result.BrokerConnectionDetailResult;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.CreateBrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.DisconnectBrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.SyncConnectionResult;
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
    private final AccountSyncBatchRepository accountSyncBatchRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final BrokerFeedPort brokerFeedPort;
    private final BrokerAccountSyncService brokerAccountSyncService;
    private final AccountDataCleanupPort accountDataCleanupPort;

    public BrokerConnectionService(
            BrokerConnectionRepository brokerConnectionRepository,
            BrokerProviderRepository brokerProviderRepository,
            AccountSyncBatchRepository accountSyncBatchRepository,
            InvestmentAccountRepository investmentAccountRepository,
            BrokerFeedPort brokerFeedPort,
            BrokerAccountSyncService brokerAccountSyncService,
            AccountDataCleanupPort accountDataCleanupPort) {
        this.brokerConnectionRepository = brokerConnectionRepository;
        this.brokerProviderRepository = brokerProviderRepository;
        this.accountSyncBatchRepository = accountSyncBatchRepository;
        this.investmentAccountRepository = investmentAccountRepository;
        this.brokerFeedPort = brokerFeedPort;
        this.brokerAccountSyncService = brokerAccountSyncService;
        this.accountDataCleanupPort = accountDataCleanupPort;
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

        // login()은 loginId/password만으로 인증하고 org는 검증하지 않는다 — 즉 클라이언트가 고른
        // brokerId(provider)와 실제로 인증된 계정의 소속 org가 다를 수 있다. 여기서 막지 않으면
        // 엉뚱한 증권사 이름표를 단 커넥션에 다른 증권사의 계좌/거래 데이터가 저장된다.
        if (!login.orgCode().equals(provider.getBrokerCode())) {
            throw new BrokerException(BrokerErrorCode.ORG_MISMATCH);
        }

        Instant connectedAt = Instant.now();
        Long connectionId = brokerConnectionRepository.insert(command.userId(), command.brokerId(), login.mockConnectionId(), connectedAt);
        Long syncBatchId = accountSyncBatchRepository.create(connectionId);

        SyncOutcome outcome = runSync(command.userId(), connectionId, login.mockConnectionId(), provider.getBrokerCode());

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
                syncBatchId, syncStatus, outcome.accountCount(), outcome.insertedTradeCount(), outcome.holdingCount(),
                outcome.errorMessage());

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

    @Transactional
    public SyncConnectionResult syncConnection(SyncBrokerConnectionCommand command) {
        BrokerConnection connection = brokerConnectionRepository.findByIdAndUserId(command.connectionId(), command.userId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.CONNECTION_NOT_FOUND));
        String mockProfileCode = brokerConnectionRepository.findMockProfileCodeByConnectionId(connection.getConnectionId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.CONNECTION_NOT_FOUND));

        Long syncBatchId = accountSyncBatchRepository.create(connection.getConnectionId());
        AccountSyncBatch createdBatch = accountSyncBatchRepository.findLatestByConnectionId(connection.getConnectionId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.CONNECTION_NOT_FOUND));

        SyncOutcome outcome = runSync(command.userId(), connection.getConnectionId(), mockProfileCode, connection.getBrokerCode());

        SyncStatus syncStatus;
        Instant completedAt = null;
        if (outcome.succeeded()) {
            accountSyncBatchRepository.markSuccess(syncBatchId);
            completedAt = Instant.now();
            brokerConnectionRepository.updateLastSyncedAt(connection.getConnectionId(), completedAt);
            syncStatus = SyncStatus.SUCCESS;
        } else {
            accountSyncBatchRepository.markFailed(syncBatchId, outcome.errorMessage());
            syncStatus = SyncStatus.FAILED;
        }

        return new SyncConnectionResult(
                syncBatchId,
                connection.getConnectionId(),
                syncStatus,
                createdBatch.getRequestedAt(),
                completedAt,
                outcome.accountCount(),
                outcome.insertedTradeCount(),
                outcome.skippedTradeCount(),
                outcome.holdingCount(),
                outcome.errorMessage()
        );
    }

    // 이미 DISCONNECTED면 데이터가 이미 지워진 상태이므로 멱등적으로 그대로 반환하고 재실행하지 않는다.
    // 그 외의 경우 계좌별로 ledger 데이터(거래/매칭/보유, 그리고 journal의 매매 근거)를 먼저 지운 뒤
    // investment_accounts를 지우고 마지막에 커넥션 상태를 전이한다 — 전부 한 트랜잭션으로 묶인다.
    @Transactional
    public DisconnectBrokerConnectionResult disconnectConnection(DisconnectBrokerConnectionCommand command) {
        BrokerConnection connection = brokerConnectionRepository.findByIdAndUserId(command.connectionId(), command.userId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.CONNECTION_NOT_FOUND));

        if (connection.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
            return new DisconnectBrokerConnectionResult(connection.getConnectionId(), ConnectionStatus.DISCONNECTED);
        }

        List<InvestmentAccount> accounts = investmentAccountRepository.findByConnectionId(connection.getConnectionId());
        for (InvestmentAccount account : accounts) {
            accountDataCleanupPort.deleteAccountData(account.getAccountId());
        }
        investmentAccountRepository.deleteByConnectionId(connection.getConnectionId());
        brokerConnectionRepository.updateStatus(connection.getConnectionId(), ConnectionStatus.DISCONNECTED);

        return new DisconnectBrokerConnectionResult(connection.getConnectionId(), ConnectionStatus.DISCONNECTED);
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

    // 계좌 목록을 조회하고, 계좌별 거래·보유 원시 데이터를 전부 미리 받아온 뒤(외부 호출,
    // 트랜잭션 없음) DB 적재는 BrokerAccountSyncService.syncAccounts에 통째로 위임한다.
    // 그 메서드가 REQUIRES_NEW라 계좌 하나라도 실패하면 이번 시도의 계좌 데이터 전체가 원자적으로
    // 롤백되고, 그 실패가 커넥션 row 자체에는 번지지 않는다 — 실패 사실만 호출자에게 돌려주면
    // (배치 상태를 FAILED로 남기기 위함) 커넥션은 CONNECTED로 유지된다.
    // fetch와 write를 분리한 이유는 BrokerAccountSyncService 상단 주석 참고 — 외부 호출을
    // DB 트랜잭션 밖으로 빼서 같은 connectionId에 대한 동시 요청이 락 대기로 타임아웃나는 문제를 없앤다.
    // createConnection/syncConnection 양쪽에서 재사용 — mockConnectionId만 있으면 되고
    // 별도 재인증 단계가 필요 없어서(client-id/secret + connectionId 방식) 하나로 합쳐도 된다.
    private SyncOutcome runSync(Long userId, Long connectionId, String mockConnectionId, String orgCode) {
        try {
            List<RawAccountRecord> accounts = brokerFeedPort.fetchAccounts(mockConnectionId, orgCode);

            List<BrokerAccountSyncService.AccountSyncBundle> bundles =
                    brokerAccountSyncService.fetchAccountBundles(mockConnectionId, accounts);

            BrokerAccountSyncService.AccountsSyncOutcome outcome =
                    brokerAccountSyncService.syncAccounts(userId, connectionId, bundles);

            return new SyncOutcome(true, null, outcome.accountCount(), outcome.insertedTradeCount(),
                    outcome.skippedTradeCount(), outcome.holdingCount());
        } catch (Exception e) {
            log.error("증권사 동기화 중 오류가 발생했습니다. connectionId={}", connectionId, e);
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new SyncOutcome(false, truncate(message, ERROR_MESSAGE_MAX_LENGTH), 0, 0, 0, 0);
        }
    }

    private String truncate(String message, int maxLength) {
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }

    private record SyncOutcome(
        boolean succeeded,
        String errorMessage,
        int accountCount,
        int insertedTradeCount,
        int skippedTradeCount,
        int holdingCount
    ) {
    }
}
