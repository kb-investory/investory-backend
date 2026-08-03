package com.investory.broker.infra.clients.mockbroker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record TransactionsResponse(
    @JsonProperty("trans_cnt") int transCnt,
    @JsonProperty("next_page") String nextPage,
    @JsonProperty("trans_list") List<TransactionItem> transList
) {
    public record TransactionItem(
        @JsonProperty("prod_name") String prodName,
        @JsonProperty("prod_code") String prodCode,
        @JsonProperty("trans_dtime") String transDtime,
        @JsonProperty("trans_no") String transNo,
        @JsonProperty("trans_type") String transType,
        @JsonProperty("trans_type_detail") String transTypeDetail,
        @JsonProperty("trans_num") BigDecimal transNum,
        @JsonProperty("trans_unit") String transUnit,
        @JsonProperty("base_amt") BigDecimal baseAmt,
        @JsonProperty("trans_amt") BigDecimal transAmt,
        @JsonProperty("settle_amt") BigDecimal settleAmt,
        @JsonProperty("balance_amt") BigDecimal balanceAmt,
        @JsonProperty("currency_code") String currencyCode,
        @JsonProperty("ex_code") String exCode
    ) {
    }
}
