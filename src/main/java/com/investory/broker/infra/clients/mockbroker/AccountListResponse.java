package com.investory.broker.infra.clients.mockbroker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AccountListResponse(
    @JsonProperty("search_timestamp") String searchTimestamp,
    @JsonProperty("account_cnt") int accountCnt,
    @JsonProperty("account_list") List<AccountListItem> accountList
) {
    public record AccountListItem(
        @JsonProperty("account_num") String accountNum,
        @JsonProperty("is_consent") boolean isConsent,
        @JsonProperty("account_name") String accountName,
        @JsonProperty("account_type") String accountType,
        @JsonProperty("issue_date") String issueDate,
        @JsonProperty("is_tax_benefits") boolean isTaxBenefits,
        @JsonProperty("is_cma") boolean isCma,
        @JsonProperty("is_stock_trans") boolean isStockTrans,
        @JsonProperty("is_account_link") boolean isAccountLink
    ) {
    }
}
