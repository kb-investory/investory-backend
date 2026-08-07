package com.investory.broker.infra.clients.mockbroker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record ProductsResponse(
    @JsonProperty("base_date") String baseDate,
    @JsonProperty("prod_cnt") int prodCnt,
    @JsonProperty("prod_list") List<ProductItem> prodList
) {
    public record ProductItem(
        @JsonProperty("prod_type") String prodType,
        @JsonProperty("prod_type_detail") String prodTypeDetail,
        @JsonProperty("prod_code") String prodCode,
        @JsonProperty("ex_code") String exCode,
        @JsonProperty("prod_name") String prodName,
        @JsonProperty("credit_type") String creditType,
        @JsonProperty("is_tax_benefits") boolean isTaxBenefits,
        @JsonProperty("purchase_amt") BigDecimal purchaseAmt,
        @JsonProperty("holding_num") BigDecimal holdingNum,
        @JsonProperty("trans_unit") String transUnit,
        @JsonProperty("eval_amt") BigDecimal evalAmt,
        @JsonProperty("is_execution") boolean isExecution,
        @JsonProperty("currency_code") String currencyCode
    ) {
    }
}
