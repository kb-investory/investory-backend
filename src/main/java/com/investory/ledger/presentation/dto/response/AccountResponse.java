package com.investory.ledger.presentation.dto.response;

import com.investory.ledger.domain.services.dto.result.AccountDetailResult;

public record AccountResponse(
    Long accountId,
    String accountName,
    String accountNumberMasked,
    String brokerageName
) {
    public static AccountResponse from(AccountDetailResult result) {
        return new AccountResponse(result.accountId(), result.accountName(),
                result.accountNumberMasked(), result.brokerageName());
    }
}
