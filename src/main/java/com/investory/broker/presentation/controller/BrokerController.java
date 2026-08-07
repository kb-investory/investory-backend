package com.investory.broker.presentation.controller;

import com.investory.broker.domain.services.BrokerConnectionService;
import com.investory.broker.domain.services.BrokerProviderService;
import com.investory.broker.domain.services.InvestmentAccountService;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.BrokerProviderResult;
import com.investory.broker.domain.services.dto.query.GetAccountDetailQuery;
import com.investory.broker.domain.services.dto.query.GetBrokerConnectionDetailQuery;
import com.investory.broker.domain.services.dto.command.SyncBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.query.GetConnectionAccountsQuery;
import com.investory.broker.domain.services.dto.result.AccountDetailResult;
import com.investory.broker.domain.services.dto.result.AccountListResult;
import com.investory.broker.domain.services.dto.result.BrokerConnectionDetailResult;
import com.investory.broker.domain.services.dto.result.ConnectionAccountsResult;
import com.investory.broker.domain.services.dto.result.CreateBrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.SyncConnectionResult;
import com.investory.broker.domain.services.dto.result.UpdateAccountNameResult;
import com.investory.broker.presentation.dto.request.CreateBrokerConnectionRequest;
import com.investory.broker.presentation.dto.request.UpdateAccountNameRequest;
import com.investory.broker.presentation.dto.response.AccountDetailResponse;
import com.investory.broker.presentation.dto.response.AccountListResponse;
import com.investory.broker.presentation.dto.response.BrokerConnectionAccountsResponse;
import com.investory.broker.presentation.dto.response.BrokerConnectionDetailResponse;
import com.investory.broker.presentation.dto.response.BrokerConnectionListResponse;
import com.investory.broker.presentation.dto.response.BrokerProviderListResponse;
import com.investory.broker.presentation.dto.response.CreateBrokerConnectionResponse;
import com.investory.broker.presentation.dto.response.SyncConnectionResponse;
import com.investory.broker.presentation.dto.response.UpdateAccountNameResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/broker")
public class BrokerController {

    // TODO: JWT 인증 도입 후 Principal.userId로 교체 (auth 도메인 미구현으로 임시 고정값 사용)
    private static final Long TEMP_USER_ID = 1L;

    private final BrokerProviderService brokerProviderService;
    private final BrokerConnectionService brokerConnectionService;
    private final InvestmentAccountService investmentAccountService;

    public BrokerController(
            BrokerProviderService brokerProviderService,
            BrokerConnectionService brokerConnectionService,
            InvestmentAccountService investmentAccountService) {
        this.brokerProviderService = brokerProviderService;
        this.brokerConnectionService = brokerConnectionService;
        this.investmentAccountService = investmentAccountService;
    }

    @GetMapping("/providers")
    public BrokerProviderListResponse getProviders() {
        List<BrokerProviderResult> results = brokerProviderService.getProviders();
        return BrokerProviderListResponse.from(results);
    }

    @GetMapping("/connections")
    public BrokerConnectionListResponse getConnections() {
        List<BrokerConnectionResult> results = brokerConnectionService.getConnections(TEMP_USER_ID);
        return BrokerConnectionListResponse.from(results);
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateBrokerConnectionResponse createConnection(@RequestBody CreateBrokerConnectionRequest request) {
        CreateBrokerConnectionResult result = brokerConnectionService.createConnection(request.toCommand(TEMP_USER_ID));
        return CreateBrokerConnectionResponse.from(result);
    }

    @GetMapping("/connections/{connectionId}")
    public BrokerConnectionDetailResponse getConnectionDetail(@PathVariable Long connectionId) {
        BrokerConnectionDetailResult result = brokerConnectionService.getConnectionDetail(
                new GetBrokerConnectionDetailQuery(TEMP_USER_ID, connectionId));
        return BrokerConnectionDetailResponse.from(result);
    }

    @GetMapping("/connections/{connectionId}/accounts")
    public BrokerConnectionAccountsResponse getConnectionAccounts(@PathVariable Long connectionId) {
        ConnectionAccountsResult result = investmentAccountService.getAccountsByConnection(
                new GetConnectionAccountsQuery(TEMP_USER_ID, connectionId));
        return BrokerConnectionAccountsResponse.from(result);
    }

    @PostMapping("/connections/{connectionId}/sync")
    public SyncConnectionResponse syncConnection(@PathVariable Long connectionId) {
        SyncConnectionResult result = brokerConnectionService.syncConnection(
                new SyncBrokerConnectionCommand(TEMP_USER_ID, connectionId));
        return SyncConnectionResponse.from(result);
    }

    @GetMapping("/accounts")
    public AccountListResponse getAccounts() {
        AccountListResult result = investmentAccountService.getAccounts(TEMP_USER_ID);
        return AccountListResponse.from(result);
    }

    @GetMapping("/accounts/{accountId}")
    public AccountDetailResponse getAccountDetail(@PathVariable Long accountId) {
        AccountDetailResult result = investmentAccountService.getAccountDetail(
                new GetAccountDetailQuery(TEMP_USER_ID, accountId));
        return AccountDetailResponse.from(result);
    }

    @PatchMapping("/accounts/{accountId}")
    public UpdateAccountNameResponse updateAccountName(
            @PathVariable Long accountId, @RequestBody UpdateAccountNameRequest request) {
        UpdateAccountNameResult result = investmentAccountService.renameAccount(request.toCommand(TEMP_USER_ID, accountId));
        return UpdateAccountNameResponse.from(result);
    }
}
