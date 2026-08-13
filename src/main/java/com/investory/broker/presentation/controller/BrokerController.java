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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public BrokerConnectionListResponse getConnections(@AuthenticationPrincipal Long userId) {
        List<BrokerConnectionResult> results = brokerConnectionService.getConnections(userId);
        return BrokerConnectionListResponse.from(results);
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateBrokerConnectionResponse createConnection(
            @AuthenticationPrincipal Long userId, @RequestBody CreateBrokerConnectionRequest request) {
        CreateBrokerConnectionResult result = brokerConnectionService.createConnection(request.toCommand(userId));
        return CreateBrokerConnectionResponse.from(result);
    }

    @GetMapping("/connections/{connectionId}")
    public BrokerConnectionDetailResponse getConnectionDetail(
            @AuthenticationPrincipal Long userId, @PathVariable Long connectionId) {
        BrokerConnectionDetailResult result = brokerConnectionService.getConnectionDetail(
                new GetBrokerConnectionDetailQuery(userId, connectionId));
        return BrokerConnectionDetailResponse.from(result);
    }

    @GetMapping("/connections/{connectionId}/accounts")
    public BrokerConnectionAccountsResponse getConnectionAccounts(
            @AuthenticationPrincipal Long userId, @PathVariable Long connectionId) {
        ConnectionAccountsResult result = investmentAccountService.getAccountsByConnection(
                new GetConnectionAccountsQuery(userId, connectionId));
        return BrokerConnectionAccountsResponse.from(result);
    }

    @PostMapping("/connections/{connectionId}/sync")
    public SyncConnectionResponse syncConnection(
            @AuthenticationPrincipal Long userId, @PathVariable Long connectionId) {
        SyncConnectionResult result = brokerConnectionService.syncConnection(
                new SyncBrokerConnectionCommand(userId, connectionId));
        return SyncConnectionResponse.from(result);
    }

    @GetMapping("/accounts")
    public AccountListResponse getAccounts(@AuthenticationPrincipal Long userId) {
        AccountListResult result = investmentAccountService.getAccounts(userId);
        return AccountListResponse.from(result);
    }

    @GetMapping("/accounts/{accountId}")
    public AccountDetailResponse getAccountDetail(
            @AuthenticationPrincipal Long userId, @PathVariable Long accountId) {
        AccountDetailResult result = investmentAccountService.getAccountDetail(
                new GetAccountDetailQuery(userId, accountId));
        return AccountDetailResponse.from(result);
    }

    @PatchMapping("/accounts/{accountId}")
    public UpdateAccountNameResponse updateAccountName(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId,
            @RequestBody UpdateAccountNameRequest request) {
        UpdateAccountNameResult result = investmentAccountService.renameAccount(request.toCommand(userId, accountId));
        return UpdateAccountNameResponse.from(result);
    }
}
