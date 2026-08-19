package com.investory.broker.infra.port_impls;

import com.investory.broker.domain.ports.AccountDataCleanupPort;
import com.investory.ledger.domain.services.AccountDataCleanupService;
import org.springframework.stereotype.Component;

@Component
public class AccountDataCleanupPortImpl implements AccountDataCleanupPort {

    private final AccountDataCleanupService accountDataCleanupService;

    public AccountDataCleanupPortImpl(AccountDataCleanupService accountDataCleanupService) {
        this.accountDataCleanupService = accountDataCleanupService;
    }

    @Override
    public void deleteAccountData(Long accountId) {
        accountDataCleanupService.deleteAccountData(accountId);
    }
}
