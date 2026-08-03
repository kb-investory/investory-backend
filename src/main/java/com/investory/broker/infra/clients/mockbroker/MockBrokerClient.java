package com.investory.broker.infra.clients.mockbroker;

import com.investory.broker.infra.clients.BrokerDataClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MockBrokerClient implements BrokerDataClient {

    private static final int TRANSACTIONS_PAGE_LIMIT = 100;
    private static final int MAX_TRANSACTION_PAGES = 50; // 무한 페이지네이션 방지용 안전장치

    private final RestTemplate restTemplate;

    @Value("${broker.mock-server.base-url}")
    private String baseUrl;

    public MockBrokerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public MockLoginResponse login(String loginId, String password) {
        HttpEntity<MockLoginRequest> entity = new HttpEntity<>(new MockLoginRequest(loginId, password), jsonHeaders());
        ResponseEntity<MockLoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/mock/auth/login", entity, MockLoginResponse.class);
        return response.getBody();
    }

    @Override
    public AccountListResponse getAccounts(String accessToken, String orgCode) {
        String url = baseUrl + "/v2/invest/accounts?org_code=" + orgCode + "&limit=500";
        HttpEntity<Void> entity = new HttpEntity<>(investHeaders(accessToken));
        ResponseEntity<AccountListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, AccountListResponse.class);
        return response.getBody();
    }

    @Override
    public AccountBasicResponse getAccountBasic(String accessToken, String accountNum) {
        HttpEntity<AccountBasicRequest> entity = new HttpEntity<>(new AccountBasicRequest(accountNum), investHeaders(accessToken));
        ResponseEntity<AccountBasicResponse> response = restTemplate.postForEntity(
                baseUrl + "/v2/invest/accounts/basic", entity, AccountBasicResponse.class);
        return response.getBody();
    }

    // next_page가 더 이상 없을 때까지 순차 조회해서 전체 거래내역을 모은다.
    @Override
    public List<TransactionsResponse.TransactionItem> getAllTransactions(String accessToken, String accountNum, String fromDate, String toDate) {
        List<TransactionsResponse.TransactionItem> all = new ArrayList<>();
        String nextPage = null;
        int page = 0;
        do {
            TransactionsRequest request = new TransactionsRequest(accountNum, fromDate, toDate, TRANSACTIONS_PAGE_LIMIT, nextPage);
            HttpEntity<TransactionsRequest> entity = new HttpEntity<>(request, investHeaders(accessToken));
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

    @Override
    public ProductsResponse getProducts(String accessToken, String accountNum) {
        HttpEntity<ProductsRequest> entity = new HttpEntity<>(new ProductsRequest(accountNum), investHeaders(accessToken));
        ResponseEntity<ProductsResponse> response = restTemplate.postForEntity(
                baseUrl + "/v2/invest/accounts/products", entity, ProductsResponse.class);
        return response.getBody();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // /v2/invest/** 공통 헤더: Authorization + 마이데이터 규격상 필요한 x-api-tran-id/x-api-type
    private HttpHeaders investHeaders(String accessToken) {
        HttpHeaders headers = jsonHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headers.add("x-api-tran-id", UUID.randomUUID().toString());
        headers.add("x-api-type", "INVESTORY");
        return headers;
    }
}
