package com.investory.broker.infra.clients;

import com.investory.broker.infra.clients.mockbroker.AccountBasicResponse;
import com.investory.broker.infra.clients.mockbroker.AccountListResponse;
import com.investory.broker.infra.clients.mockbroker.MockLoginResponse;
import com.investory.broker.infra.clients.mockbroker.ProductsResponse;
import com.investory.broker.infra.clients.mockbroker.TransactionsResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class FakeBrokerDataClient implements BrokerDataClient {

    private MockLoginResponse loginResponse = new MockLoginResponse(
            "conn-1", "access-token", "Bearer", "S9990001A", "미래에셋증권(모의)", List.of());
    private boolean loginAuthFails = false;
    private boolean loginServerFails = false;

    private AccountListResponse accountListResponse = new AccountListResponse("", 0, List.of());
    private AccountBasicResponse accountBasicResponse = new AccountBasicResponse("", "", 0, List.of(
            new AccountBasicResponse.AccountBasicItem("KRW", null, null, null, null)));
    private List<TransactionsResponse.TransactionItem> transactions = List.of();
    private ProductsResponse productsResponse = new ProductsResponse("20260730", 0, List.of());
    private RuntimeException syncFailure;

    public void willFailLoginWithUnauthorized() {
        this.loginAuthFails = true;
    }

    public void willFailLoginWithServerError() {
        this.loginServerFails = true;
    }

    public void willReturnAccounts(AccountListResponse response) {
        this.accountListResponse = response;
    }

    public void willReturnTransactions(List<TransactionsResponse.TransactionItem> transactions) {
        this.transactions = transactions;
    }

    public void willReturnProducts(ProductsResponse response) {
        this.productsResponse = response;
    }

    public void willFailSyncWith(RuntimeException exception) {
        this.syncFailure = exception;
    }

    @Override
    public MockLoginResponse login(String loginId, String password) {
        if (loginAuthFails) {
            throw HttpClientErrorException.create(
                    HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        }
        if (loginServerFails) {
            throw new ResourceAccessException("연결할 수 없습니다.");
        }
        return loginResponse;
    }

    @Override
    public AccountListResponse getAccounts(String accessToken, String orgCode) {
        if (syncFailure != null) {
            throw syncFailure;
        }
        return accountListResponse;
    }

    @Override
    public AccountBasicResponse getAccountBasic(String accessToken, String accountNum) {
        return accountBasicResponse;
    }

    @Override
    public List<TransactionsResponse.TransactionItem> getAllTransactions(String accessToken, String accountNum, String fromDate, String toDate) {
        return transactions;
    }

    @Override
    public ProductsResponse getProducts(String accessToken, String accountNum) {
        return productsResponse;
    }
}
