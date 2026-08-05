package com.investory.broker.domain.repositories;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.model.InvestmentAccount;

import java.util.List;
import java.util.Optional;

public interface InvestmentAccountRepository {
    // (connectionId, externalAccountId) 기준 upsert — 최초 연동/재동기화 양쪽에서 재사용하기 위해
    // 이미 존재하는 계좌는 표시 정보만 갱신하고 기존 accountId를 그대로 반환한다.
    Long upsert(
            Long connectionId,
            String externalAccountId,
            String accountNoMasked,
            String accountName,
            AccountType accountType,
            String currencyCode
    );

    List<InvestmentAccount> findByConnectionId(Long connectionId);

    List<InvestmentAccount> findByUserId(Long userId);

    List<InvestmentAccount> findByIds(List<Long> accountIds);

    Optional<InvestmentAccount> findByIdAndUserId(Long accountId, Long userId);

    void updateAccountName(Long accountId, String accountName);
}
