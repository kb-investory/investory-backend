package com.kbinvestory.backend.account.presentation.controller;

import com.kbinvestory.backend.account.domain.services.BrokerService;
import com.kbinvestory.backend.account.domain.services.dto.result.BrokerResult;
import com.kbinvestory.backend.account.presentation.dto.request.GetBrokersRequest;
import com.kbinvestory.backend.account.presentation.dto.response.BrokerResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/brokers")
public class BrokerController {

    private final BrokerService brokerService;

    public BrokerController(BrokerService brokerService) {
        this.brokerService = brokerService;
    }

    @GetMapping
    public List<BrokerResponse> getBrokers(@ModelAttribute GetBrokersRequest request) {
        List<BrokerResult> results = brokerService.getBrokers(request.toQuery());
        return results.stream()
                .map(BrokerResponse::from)
                .collect(Collectors.toList());
    }
}