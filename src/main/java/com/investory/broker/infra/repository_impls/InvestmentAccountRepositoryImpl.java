package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.infra.entities.InvestmentAccountRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.InvestmentAccountMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

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
}
