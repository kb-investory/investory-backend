package com.kbinvestory.backend.account.domain.services;

import com.kbinvestory.backend.account.domain.constant.ConnectionStatus;
import com.kbinvestory.backend.account.domain.exception.AccountConnectionException;
import com.kbinvestory.backend.account.domain.exception.AccountErrorCode;
import com.kbinvestory.backend.account.domain.model.AccountConnection;
import com.kbinvestory.backend.account.domain.model.BrokerageProvider;
import com.kbinvestory.backend.account.domain.ports.BrokerAuthPort;
import com.kbinvestory.backend.account.domain.ports.dto.BrokerAuthInfo;
import com.kbinvestory.backend.account.domain.repositories.AccountConnectionRepository;
import com.kbinvestory.backend.account.domain.repositories.BrokerageProviderRepository;
import com.kbinvestory.backend.account.domain.services.dto.command.CreateBrokerConnectionCommand;
import com.kbinvestory.backend.account.domain.services.dto.result.BrokerConnectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountConnectionService {

    private static final Logger log = LoggerFactory.getLogger(AccountConnectionService.class);

    private final BrokerageProviderRepository brokerageProviderRepository;
    private final AccountConnectionRepository accountConnectionRepository;
    private final BrokerAuthPort brokerAuthPort;

    public AccountConnectionService(BrokerageProviderRepository brokerageProviderRepository,
                                     AccountConnectionRepository accountConnectionRepository,
                                     BrokerAuthPort brokerAuthPort) {
        this.brokerageProviderRepository = brokerageProviderRepository;
        this.accountConnectionRepository = accountConnectionRepository;
        this.brokerAuthPort = brokerAuthPort;
    }

    public BrokerConnectionResult connect(CreateBrokerConnectionCommand command) {
        BrokerageProvider provider = brokerageProviderRepository.findById(command.providerId())
                .orElseThrow(() -> new AccountConnectionException(AccountErrorCode.PROVIDER_NOT_FOUND));

        accountConnectionRepository.findByUserIdAndProviderId(command.userId(), command.providerId())
                .filter(existing -> existing.getConnectionStatus() == ConnectionStatus.CONNECTED)
                .ifPresent(existing -> {
                    throw new AccountConnectionException(AccountErrorCode.ALREADY_CONNECTED);
                });

        BrokerAuthInfo authInfo = brokerAuthPort.authenticate(provider.getCode(), command.loginId(), command.password());
        if (!authInfo.success()) {
            log.warn("증권사 인증 실패: providerId={}, reason={}", provider.getId(), authInfo.failureReason());
            throw new AccountConnectionException(AccountErrorCode.BROKER_AUTH_FAILED);
        }

        AccountConnection connection = AccountConnection.create(command.userId(), provider.getId(), authInfo.connectedId());
        AccountConnection saved = accountConnectionRepository.save(connection);
        return BrokerConnectionResult.from(saved);
    }
}