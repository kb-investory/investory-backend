package com.investory.broker.infra.clients.mockbroker;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransactionsRequest(
    @JsonProperty("account_num") String accountNum,
    @JsonProperty("from_date") String fromDate,
    @JsonProperty("to_date") String toDate,
    Integer limit,
    @JsonProperty("next_page") String nextPage
) {
}
