package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.infra.entities.InvestmentAccountRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.InvestmentAccountMapper;
import com.investory.core.exception.ErrorType;
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
    public Long upsert(
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
            investmentAccountMapper.upsert(row);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "계좌 정보를 저장하는 중 오류가 발생했습니다.", e);
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
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "연결의 계좌 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<InvestmentAccount> findByUserId(Long userId) {
        try {
            return investmentAccountMapper.findByUserId(userId).stream()
                    .map(InvestmentAccountRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "계좌 목록을 조회하는 중 오류가 발생했습니다.", e);
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
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "accountId 목록으로 계좌를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<InvestmentAccount> findByIdAndUserId(Long accountId, Long userId) {
        try {
            return investmentAccountMapper.findByIdAndUserId(accountId, userId).stream()
                    .map(InvestmentAccountRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "계좌를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void updateAccountName(Long accountId, String accountName) {
        try {
            investmentAccountMapper.updateAccountName(accountId, accountName);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "계좌 이름을 변경하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteByConnectionId(Long connectionId) {
        try {
            investmentAccountMapper.deleteByConnectionId(connectionId);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "계좌를 삭제하는 중 오류가 발생했습니다.", e);
        }
    }
}
