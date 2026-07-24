package com.kbinvestory.backend.account.presentation.controller;

import com.kbinvestory.backend.account.domain.services.AccountConnectionService;
import com.kbinvestory.backend.account.domain.services.dto.result.BrokerConnectionResult;
import com.kbinvestory.backend.account.presentation.dto.request.CreateBrokerConnectionRequest;
import com.kbinvestory.backend.account.presentation.dto.response.BrokerConnectionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/broker-connections")
public class BrokerConnectionController {

    private final AccountConnectionService accountConnectionService;

    public BrokerConnectionController(AccountConnectionService accountConnectionService) {
        this.accountConnectionService = accountConnectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrokerConnectionResponse connect(@RequestBody CreateBrokerConnectionRequest request) {
        BrokerConnectionResult result = accountConnectionService.connect(request.toCommand());
        return BrokerConnectionResponse.from(result);
    }
}
