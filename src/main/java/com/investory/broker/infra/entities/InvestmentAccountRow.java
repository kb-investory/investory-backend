package com.investory.broker.infra.entities;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.model.InvestmentAccount;
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

    public InvestmentAccount toDomain() {
        return InvestmentAccount.of(
                accountId, connectionId, externalAccountId, accountNoMasked,
                accountName, AccountType.valueOf(accountType), currencyCode
        );
    }
}
