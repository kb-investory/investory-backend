package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.services.dto.result.UpdateAccountNameResult;

public record UpdateAccountNameResponse(
    Long accountId,
    String accountNoMasked,
    String accountName,
    AccountType accountType
) {
    public static UpdateAccountNameResponse from(UpdateAccountNameResult result) {
        return new UpdateAccountNameResponse(
                result.accountId(), result.accountNoMasked(), result.accountName(), result.accountType());
    }
}
