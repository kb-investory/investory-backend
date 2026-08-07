package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.AccountType;

// broker 외부(예: ledger의 AccountPort)로 계좌 표시 정보를 노출할 때 쓰는 결과 타입.
public record InvestmentAccountResult(
    Long accountId,
    Long connectionId,
    String accountNoMasked,
    String accountName,
    AccountType accountType,
    String currencyCode,
    String brokerName
) {
}
