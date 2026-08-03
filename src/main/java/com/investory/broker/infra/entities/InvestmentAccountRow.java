package com.investory.broker.infra.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InvestmentAccountRow {
    private Long accountId;
    private Long connectionId;
    private String externalAccountId;
    private String accountNoMasked;
    private String accountName;
    private String accountType;
    private String currencyCode;
}
