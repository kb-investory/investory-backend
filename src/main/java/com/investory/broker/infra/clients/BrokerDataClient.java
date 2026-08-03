package com.investory.broker.infra.clients;

import com.investory.broker.infra.clients.mockbroker.AccountBasicResponse;
import com.investory.broker.infra.clients.mockbroker.AccountListResponse;
import com.investory.broker.infra.clients.mockbroker.MockLoginResponse;
import com.investory.broker.infra.clients.mockbroker.ProductsResponse;
import com.investory.broker.infra.clients.mockbroker.TransactionsResponse;

import java.util.List;

// 증권사 데이터 소스(목 서버, 추후 실증권사 등)를 추상화한 클라이언트 경계.
// BrokerConnectionService는 이 인터페이스에만 의존해서 테스트에서 FakeBrokerDataClient로 대체할 수 있다.
public interface BrokerDataClient {
    MockLoginResponse login(String loginId, String password);

    AccountListResponse getAccounts(String accessToken, String orgCode);

    AccountBasicResponse getAccountBasic(String accessToken, String accountNum);

    List<TransactionsResponse.TransactionItem> getAllTransactions(String accessToken, String accountNum, String fromDate, String toDate);

    ProductsResponse getProducts(String accessToken, String accountNum);
}
