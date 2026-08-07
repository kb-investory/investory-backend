package com.investory.ledger.domain.ports.dto;

public record AccountInfo(
    Long accountId,
    String accountName,
    String accountNumberMasked,
    String brokerageName
) {
}
