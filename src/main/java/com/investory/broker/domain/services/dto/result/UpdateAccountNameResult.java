package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.AccountType;

public record UpdateAccountNameResult(
    Long accountId,
    String accountNoMasked,
    String accountName,
    AccountType accountType
) {
}
