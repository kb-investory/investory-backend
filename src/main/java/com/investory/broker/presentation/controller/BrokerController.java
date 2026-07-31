package com.investory.broker.presentation.controller;

import com.investory.broker.domain.services.BrokerProviderService;
import com.investory.broker.domain.services.dto.result.BrokerProviderResult;
import com.investory.broker.presentation.dto.response.BrokerProviderListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/broker")
public class BrokerController {

    private final BrokerProviderService brokerProviderService;

    public BrokerController(BrokerProviderService brokerProviderService) {
        this.brokerProviderService = brokerProviderService;
    }

    @GetMapping("/providers")
    public BrokerProviderListResponse getProviders() {
        List<BrokerProviderResult> results = brokerProviderService.getProviders();
        return BrokerProviderListResponse.from(results);
    }
}