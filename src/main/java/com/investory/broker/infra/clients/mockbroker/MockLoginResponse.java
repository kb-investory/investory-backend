package com.investory.broker.infra.clients.mockbroker;

import java.math.BigDecimal;
import java.util.List;

public record MockLoginResponse(
    String connectionId,
    String accessToken,
    String tokenType,
    String orgCode,
    String orgName,
    List<AccountSummary> accounts
) {
    public record AccountSummary(
        String accountNum,
        String accountName,
        String accountType,
        String issueDate,
        BigDecimal cashBalance
    ) {
    }
}
