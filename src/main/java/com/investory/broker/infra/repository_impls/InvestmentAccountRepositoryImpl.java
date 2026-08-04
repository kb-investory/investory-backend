package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.infra.entities.InvestmentAccountRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.InvestmentAccountMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InvestmentAccountRepositoryImpl implements InvestmentAccountRepository {

    private final InvestmentAccountMapper investmentAccountMapper;

    public InvestmentAccountRepositoryImpl(InvestmentAccountMapper investmentAccountMapper) {
        this.investmentAccountMapper = investmentAccountMapper;
    }

    @Override
    public Long insert(
            Long connectionId,
            String externalAccountId,
            String accountNoMasked,
            String accountName,
            AccountType accountType,
            String currencyCode) {
        InvestmentAccountRow row = new InvestmentAccountRow();
        row.setConnectionId(connectionId);
        row.setExternalAccountId(externalAccountId);
        row.setAccountNoMasked(accountNoMasked);
        row.setAccountName(accountName);
        row.setAccountType(accountType.name());
        row.setCurrencyCode(currencyCode);
        try {
            investmentAccountMapper.insert(row);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
        return row.getAccountId();
    }

    @Override
    public List<InvestmentAccount> findByConnectionId(Long connectionId) {
        try {
            return investmentAccountMapper.findByConnectionId(connectionId).stream()
                    .map(InvestmentAccountRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }

    @Override
    public List<InvestmentAccount> findByUserId(Long userId) {
        try {
            return investmentAccountMapper.findByUserId(userId).stream()
                    .map(InvestmentAccountRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }

    @Override
    public List<InvestmentAccount> findByIds(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        try {
            return investmentAccountMapper.findByIds(accountIds).stream()
                    .map(InvestmentAccountRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }

    @Override
    public Optional<InvestmentAccount> findByIdAndUserId(Long accountId, Long userId) {
        try {
            return investmentAccountMapper.findByIdAndUserId(accountId, userId).stream()
                    .map(InvestmentAccountRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }
}
