package com.investory.ledger.infra.port_impls;

import com.investory.broker.domain.services.AccountLookupService;
import com.investory.broker.domain.services.dto.result.InvestmentAccountResult;
import com.investory.ledger.domain.ports.AccountPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AccountPortImpl implements AccountPort {

    private final AccountLookupService accountLookupService;

    public AccountPortImpl(AccountLookupService accountLookupService) {
        this.accountLookupService = accountLookupService;
    }

    @Override
    public List<AccountInfo> findAccountsByUserId(Long userId) {
        return accountLookupService.findByUserId(userId).stream()
                .map(this::toAccountInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountInfo> findAccounts(List<Long> accountIds) {
        return accountLookupService.findByIds(accountIds).stream()
                .map(this::toAccountInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AccountInfo> findAccount(Long accountId, Long userId) {
        return accountLookupService.findByIdAndUserId(accountId, userId)
                .map(this::toAccountInfo);
    }

    private AccountInfo toAccountInfo(InvestmentAccountResult result) {
        return new AccountInfo(result.accountId(), result.accountName(), result.accountNoMasked(), result.brokerName());
    }
}
