package com.investory.broker.presentation.controller;

import com.investory.broker.domain.services.BrokerConnectionService;
import com.investory.broker.domain.services.BrokerProviderService;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.BrokerProviderResult;
import com.investory.broker.presentation.dto.response.BrokerConnectionListResponse;
import com.investory.broker.presentation.dto.response.BrokerProviderListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/broker")
public class BrokerController {

    // TODO: JWT 인증 도입 후 Principal.userId로 교체 (auth 도메인 미구현으로 임시 고정값 사용)
    private static final Long TEMP_USER_ID = 1L;

    private final BrokerProviderService brokerProviderService;
    private final BrokerConnectionService brokerConnectionService;

    public BrokerController(BrokerProviderService brokerProviderService, BrokerConnectionService brokerConnectionService) {
        this.brokerProviderService = brokerProviderService;
        this.brokerConnectionService = brokerConnectionService;
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
}