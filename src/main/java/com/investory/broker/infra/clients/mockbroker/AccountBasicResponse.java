package com.investory.broker.infra.clients.mockbroker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record AccountBasicResponse(
    @JsonProperty("search_timestamp") String searchTimestamp,
    @JsonProperty("base_date") String baseDate,
    @JsonProperty("basic_cnt") int basicCnt,
    @JsonProperty("basic_list") List<AccountBasicItem> basicList
) {
    public record AccountBasicItem(
        @JsonProperty("currency_code") String currencyCode,
        @JsonProperty("withholdings_amt") BigDecimal withholdingsAmt,
        @JsonProperty("credit_loan_amt") BigDecimal creditLoanAmt,
        @JsonProperty("mortgage_amt") BigDecimal mortgageAmt,
        @JsonProperty("avail_balance") BigDecimal availBalance
    ) {
    }
}
