package com.investory.ledger.domain.services.dto.result;

public record AccountDetailResult(
    Long accountId,
    String accountName,
    String accountNumberMasked,
    String brokerageName
) {
}
