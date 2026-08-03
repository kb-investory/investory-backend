package com.investory.broker.domain.services;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.exception.BrokerErrorCode;
import com.investory.broker.domain.exception.BrokerException;
import com.investory.broker.domain.model.BrokerProvider;
import com.investory.broker.domain.ports.HoldingIngestionPort;
import com.investory.broker.domain.ports.TradeIngestionPort;
import com.investory.broker.domain.ports.dto.IngestResult;
import com.investory.broker.domain.ports.dto.RawHoldingRecord;
import com.investory.broker.domain.ports.dto.RawTradeRecord;
import com.investory.broker.domain.repositories.AccountSyncBatchRepository;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.repositories.BrokerProviderRepository;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.domain.services.dto.command.CreateBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.CreateBrokerConnectionResult;
import com.investory.broker.infra.clients.BrokerDataClient;
import com.investory.broker.infra.clients.mockbroker.AccountBasicResponse;
import com.investory.broker.infra.clients.mockbroker.AccountListResponse;
import com.investory.broker.infra.clients.mockbroker.MockLoginResponse;
import com.investory.broker.infra.clients.mockbroker.ProductsResponse;
import com.investory.broker.infra.clients.mockbroker.TransactionsResponse;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.core.exception.ErrorType;
import com.investory.core.exception.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrokerConnectionService {

    private static final Logger log = LoggerFactory.getLogger(BrokerConnectionService.class);

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TRANS_DTIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter YYYYMMDD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String EARLIEST_FROM_DATE = "20000101";
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final BrokerConnectionRepository brokerConnectionRepository;
    private final BrokerProviderRepository brokerProviderRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final AccountSyncBatchRepository accountSyncBatchRepository;
    private final TradeIngestionPort tradeIngestionPort;
    private final HoldingIngestionPort holdingIngestionPort;
    private final BrokerDataClient brokerDataClient;

    public BrokerConnectionService(
            BrokerConnectionRepository brokerConnectionRepository,
            BrokerProviderRepository brokerProviderRepository,
            InvestmentAccountRepository investmentAccountRepository,
            AccountSyncBatchRepository accountSyncBatchRepository,
            TradeIngestionPort tradeIngestionPort,
            HoldingIngestionPort holdingIngestionPort,
            BrokerDataClient brokerDataClient) {
        this.brokerConnectionRepository = brokerConnectionRepository;
        this.brokerProviderRepository = brokerProviderRepository;
        this.investmentAccountRepository = investmentAccountRepository;
        this.accountSyncBatchRepository = accountSyncBatchRepository;
        this.tradeIngestionPort = tradeIngestionPort;
        this.holdingIngestionPort = holdingIngestionPort;
        this.brokerDataClient = brokerDataClient;
    }

    public List<BrokerConnectionResult> getConnections(Long userId) {
        return brokerConnectionRepository.findAllByUserId(userId).stream()
                .map(BrokerConnectionResult::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CreateBrokerConnectionResult createConnection(CreateBrokerConnectionCommand command) {
        validate(command);

        BrokerProvider provider = brokerProviderRepository.findById(command.brokerId())
                .orElseThrow(() -> new BrokerException(BrokerErrorCode.PROVIDER_NOT_FOUND));

        if (brokerConnectionRepository.findActiveByUserIdAndBrokerId(command.userId(), command.brokerId()).isPresent()) {
            throw new BrokerException(BrokerErrorCode.ALREADY_CONNECTED);
        }

        MockLoginResponse login = authenticate(command.loginId(), command.password());

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

    private MockLoginResponse authenticate(String loginId, String password) {
        try {
            return brokerDataClient.login(loginId, password);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new BrokerException(BrokerErrorCode.BROKER_AUTH_FAILED);
        } catch (RestClientException e) {
            throw new BrokerInfraException(ErrorType.EXTERNAL_ERROR, "목 증권사 서버 인증 중 오류가 발생했습니다.", e);
        }
    }

    // 계좌 목록부터 계좌별 거래내역/보유상품까지 조회해서 investment_accounts에 저장하고,
    // 거래/보유종목은 ledger 쪽 Port로 저장 요청만 넘긴다. 도중에 실패해도 커넥션 자체는 롤백하지 않고
    // 실패 사실만 호출자에게 돌려준다 (배치 상태를 FAILED로 남기기 위함).
    private SyncOutcome runSync(Long userId, Long connectionId, String accessToken, String orgCode) {
        try {
            AccountListResponse accountList = brokerDataClient.getAccounts(accessToken, orgCode);
            String toDate = LocalDate.now(SEOUL_ZONE).format(YYYYMMDD_FORMAT);

            int accountCount = 0;
            int insertedTradeCount = 0;
            int holdingCount = 0;

            for (AccountListResponse.AccountListItem item : accountList.accountList()) {
                Long accountId = createInvestmentAccount(accessToken, connectionId, item);
                accountCount++;

                List<TransactionsResponse.TransactionItem> transactions =
                        brokerDataClient.getAllTransactions(accessToken, item.accountNum(), EARLIEST_FROM_DATE, toDate);
                List<RawTradeRecord> rawTrades = transactions.stream()
                        .map(this::toRawTradeRecord)
                        .collect(Collectors.toList());
                IngestResult tradeResult = tradeIngestionPort.ingestTrades(userId, accountId, rawTrades);
                insertedTradeCount += tradeResult.successCount();

                ProductsResponse products = brokerDataClient.getProducts(accessToken, item.accountNum());
                List<RawHoldingRecord> rawHoldings = products.prodList().stream()
                        .map(this::toRawHoldingRecord)
                        .collect(Collectors.toList());
                IngestResult holdingResult = holdingIngestionPort.ingestHoldings(
                        userId, accountId, parseYyyyMmDd(products.baseDate()), rawHoldings);
                holdingCount += holdingResult.successCount();
            }

            return new SyncOutcome(true, null, accountCount, insertedTradeCount, holdingCount);
        } catch (Exception e) {
            log.error("증권사 동기화 중 오류가 발생했습니다. connectionId={}", connectionId, e);
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new SyncOutcome(false, truncate(message, ERROR_MESSAGE_MAX_LENGTH), 0, 0, 0);
        }
    }

    private Long createInvestmentAccount(String accessToken, Long connectionId, AccountListResponse.AccountListItem item) {
        AccountBasicResponse basic = brokerDataClient.getAccountBasic(accessToken, item.accountNum());
        String currencyCode = !basic.basicList().isEmpty() ? basic.basicList().get(0).currencyCode() : "KRW";

        return investmentAccountRepository.insert(
                connectionId,
                item.accountNum(),
                maskAccountNo(item.accountNum()),
                item.accountName(),
                mapAccountType(item.accountType()),
                currencyCode
        );
    }

    private RawTradeRecord toRawTradeRecord(TransactionsResponse.TransactionItem item) {
        String tradeSide = item.transTypeDetail() != null && item.transTypeDetail().contains("매수") ? "BUY" : "SELL";
        BigDecimal transactionCostAmount = item.transAmt().subtract(item.settleAmt()).abs();
        Instant tradedAt = LocalDateTime.parse(item.transDtime(), TRANS_DTIME_FORMAT).atZone(SEOUL_ZONE).toInstant();
        return new RawTradeRecord(
                item.transNo(),
                item.prodCode(),
                tradeSide,
                item.transNum(),
                item.baseAmt(),
                transactionCostAmount,
                tradedAt
        );
    }

    private RawHoldingRecord toRawHoldingRecord(ProductsResponse.ProductItem item) {
        BigDecimal holdingNum = item.holdingNum();
        BigDecimal averagePurchasePrice = BigDecimal.ZERO;
        BigDecimal currentPrice = BigDecimal.ZERO;
        if (holdingNum != null && holdingNum.compareTo(BigDecimal.ZERO) != 0) {
            averagePurchasePrice = item.purchaseAmt().divide(holdingNum, 4, RoundingMode.HALF_UP);
            currentPrice = item.evalAmt().divide(holdingNum, 4, RoundingMode.HALF_UP);
        }
        return new RawHoldingRecord(item.prodCode(), holdingNum, averagePurchasePrice, currentPrice);
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

    private LocalDate parseYyyyMmDd(String yyyymmdd) {
        return LocalDate.parse(yyyymmdd, YYYYMMDD_FORMAT);
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
