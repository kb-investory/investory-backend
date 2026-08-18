package com.investory.broker.infra.clients.mockbroker;

import com.investory.broker.domain.ports.BrokerFeedPort;
import com.investory.broker.domain.ports.dto.BrokerLoginResult;
import com.investory.broker.domain.ports.dto.RawAccountRecord;
import com.investory.broker.domain.ports.dto.RawHoldingBatch;
import com.investory.broker.domain.ports.dto.RawHoldingRecord;
import com.investory.broker.domain.ports.dto.RawOrganizationRecord;
import com.investory.broker.domain.ports.dto.RawTradeRecord;
import com.investory.broker.infra.exception.BrokerFeedAuthFailedException;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.core.exception.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// 목 증권사 서버(마이데이터 규격 /v2/invest/**)를 실제로 호출하는 유일한 클래스.
// raw JSON 파싱/페이지네이션/필드 매핑을 전부 여기서 끝내고, BrokerFeedPort 밖으로는
// 순수 도메인 DTO(BrokerLoginResult, RawXxxRecord)만 나간다 — domain은 이 클래스의
// 존재도, 목 서버 응답 포맷도 몰라야 한다.
//
// 인증은 client-id/secret + connectionId 방식(외부 서비스 연동)만 쓴다 — 유저 본인의
// accessToken/Bearer 방식은 쓰지 않는다. login()이 발급받는 connectionId를 저장해두면
// 이후 fetchXxx 호출은 비밀번호 없이 그 connectionId 재사용만으로 계속 가능하다.
@Component
public class MockBrokerFeedClient implements BrokerFeedPort {

    private static final int TRANSACTIONS_PAGE_LIMIT = 100;
    private static final int MAX_TRANSACTION_PAGES = 50; // 무한 페이지네이션 방지용 안전장치
    private static final String EARLIEST_FROM_DATE = "20000101";

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TRANS_DTIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter YYYYMMDD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate;

    @Value("${broker.mock-server.base-url}")
    private String baseUrl;

    @Value("${broker.mock-server.client-id}")
    private String clientId;

    @Value("${broker.mock-server.client-secret}")
    private String clientSecret;

    public MockBrokerFeedClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // POST /mock/system/connections — client-id/secret으로 스스로를 증명하고 loginId/password로
    // 그 유저와의 커넥션을 발급받는다. 같은 유저에 다시 요청해도 같은 connectionId가 재발급되므로
    // 이 결과의 mockConnectionId를 저장해두면 이후 재동기화 시 이 메서드를 다시 부를 필요가 없다.
    @Override
    public BrokerLoginResult login(String loginId, String password) {
        try {
            HttpHeaders headers = jsonHeaders();
            headers.add("x-client-id", clientId);
            headers.add("x-client-secret", clientSecret);
            HttpEntity<MockLoginRequest> entity = new HttpEntity<>(new MockLoginRequest(loginId, password), headers);
            ResponseEntity<MockLoginResponse> response = restTemplate.postForEntity(
                    baseUrl + "/mock/system/connections", entity, MockLoginResponse.class);
            MockLoginResponse body = response.getBody();
            return new BrokerLoginResult(body.connectionId(), body.orgCode(), body.orgName());
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new BrokerFeedAuthFailedException(e);
        } catch (RestClientException e) {
            throw new BrokerInfraException(ErrorType.EXTERNAL_ERROR, "목 증권사 서버 인증 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<RawAccountRecord> fetchAccounts(String mockConnectionId, String orgCode) {
        try {
            String url = baseUrl + "/v2/invest/accounts?org_code=" + orgCode + "&limit=500";
            HttpEntity<Void> entity = new HttpEntity<>(investHeaders(mockConnectionId));
            ResponseEntity<AccountListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AccountListResponse.class);
            AccountListResponse body = response.getBody();
            if (body == null || body.accountList() == null) {
                return List.of();
            }
            return body.accountList().stream()
                    .map(item -> new RawAccountRecord(
                            item.accountNum(), item.accountName(), item.accountType(),
                            fetchCurrencyCode(mockConnectionId, item.accountNum())))
                    .collect(Collectors.toList());
        } catch (RestClientException e) {
            throw new BrokerInfraException(ErrorType.EXTERNAL_ERROR, "증권사 계좌 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<RawTradeRecord> fetchTrades(String mockConnectionId, String accountNum) {
        try {
            String toDate = LocalDate.now(SEOUL_ZONE).format(YYYYMMDD_FORMAT);
            return getAllTransactions(mockConnectionId, accountNum, EARLIEST_FROM_DATE, toDate).stream()
                    .map(this::toRawTradeRecord)
                    .collect(Collectors.toList());
        } catch (RestClientException e) {
            throw new BrokerInfraException(ErrorType.EXTERNAL_ERROR, "증권사 거래내역을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public RawHoldingBatch fetchHoldings(String mockConnectionId, String accountNum) {
        try {
            HttpEntity<ProductsRequest> entity = new HttpEntity<>(new ProductsRequest(accountNum), investHeaders(mockConnectionId));
            ResponseEntity<ProductsResponse> response = restTemplate.postForEntity(
                    baseUrl + "/v2/invest/accounts/products", entity, ProductsResponse.class);
            ProductsResponse body = response.getBody();
            List<RawHoldingRecord> holdings = body.prodList() == null ? List.of() : body.prodList().stream()
                    .map(this::toRawHoldingRecord)
                    .collect(Collectors.toList());
            return new RawHoldingBatch(LocalDate.parse(body.baseDate(), YYYYMMDD_FORMAT), holdings);
        } catch (RestClientException e) {
            throw new BrokerInfraException(ErrorType.EXTERNAL_ERROR, "증권사 보유종목을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    // GET /mock/system/orgs — /mock/system/connections(login)과 마찬가지로 client-id/secret만
    // 증명하면 되는 시스템 API. 특정 유저의 connectionId가 필요 없어 investHeaders() 대신
    // jsonHeaders()+client-id/secret 조합을 그대로 재사용한다.
    @Override
    public List<RawOrganizationRecord> fetchOrganizations() {
        try {
            HttpHeaders headers = jsonHeaders();
            headers.add("x-client-id", clientId);
            headers.add("x-client-secret", clientSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List<OrgItem>> response = restTemplate.exchange(
                    baseUrl + "/mock/system/orgs", HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<OrgItem>>() {});
            List<OrgItem> body = response.getBody();
            if (body == null) {
                return List.of();
            }
            return body.stream()
                    .map(item -> new RawOrganizationRecord(item.orgCode(), item.orgName()))
                    .collect(Collectors.toList());
        } catch (RestClientException e) {
            throw new BrokerInfraException(ErrorType.EXTERNAL_ERROR, "증권사 기관 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    private String fetchCurrencyCode(String mockConnectionId, String accountNum) {
        HttpEntity<AccountBasicRequest> entity = new HttpEntity<>(new AccountBasicRequest(accountNum), investHeaders(mockConnectionId));
        ResponseEntity<AccountBasicResponse> response = restTemplate.postForEntity(
                baseUrl + "/v2/invest/accounts/basic", entity, AccountBasicResponse.class);
        AccountBasicResponse body = response.getBody();
        if (body == null || body.basicList() == null || body.basicList().isEmpty()) {
            return "KRW";
        }
        return body.basicList().get(0).currencyCode();
    }

    // next_page가 더 이상 없을 때까지 순차 조회해서 전체 거래내역을 모은다.
    private List<TransactionsResponse.TransactionItem> getAllTransactions(String mockConnectionId, String accountNum, String fromDate, String toDate) {
        List<TransactionsResponse.TransactionItem> all = new ArrayList<>();
        String nextPage = null;
        int page = 0;
        do {
            TransactionsRequest request = new TransactionsRequest(accountNum, fromDate, toDate, TRANSACTIONS_PAGE_LIMIT, nextPage);
            HttpEntity<TransactionsRequest> entity = new HttpEntity<>(request, investHeaders(mockConnectionId));
            ResponseEntity<TransactionsResponse> response = restTemplate.postForEntity(
                    baseUrl + "/v2/invest/accounts/transactions", entity, TransactionsResponse.class);
            TransactionsResponse body = response.getBody();
            if (body == null || body.transList() == null) {
                break;
            }
            all.addAll(body.transList());
            nextPage = body.nextPage();
            page++;
        } while (nextPage != null && page < MAX_TRANSACTION_PAGES);
        return all;
    }

    private RawTradeRecord toRawTradeRecord(TransactionsResponse.TransactionItem item) {
        String tradeSide = item.transTypeDetail() != null && item.transTypeDetail().contains("매수") ? "BUY" : "SELL";
        BigDecimal transactionCostAmount = item.transAmt().subtract(item.settleAmt()).abs();
        Instant tradedAt = LocalDateTime.parse(item.transDtime(), TRANS_DTIME_FORMAT).atZone(SEOUL_ZONE).toInstant();
        return new RawTradeRecord(
                item.transNo(), item.prodCode(), tradeSide,
                item.transNum(), item.baseAmt(), transactionCostAmount, tradedAt
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

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // /mock/**(auth/system 제외)·/v2/invest/** 공통 헤더: client-id/secret + connection-id
    // (마이데이터 규격상 x-api-tran-id/x-api-type도 함께 실어 보낸다)
    private HttpHeaders investHeaders(String mockConnectionId) {
        HttpHeaders headers = jsonHeaders();
        headers.add("x-client-id", clientId);
        headers.add("x-client-secret", clientSecret);
        headers.add("x-connection-id", mockConnectionId);
        headers.add("x-api-tran-id", UUID.randomUUID().toString());
        headers.add("x-api-type", "INVESTORY");
        return headers;
    }
}
