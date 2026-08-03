package com.investory.broker.infra.clients.mockbroker;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductsRequest(
    @JsonProperty("account_num") String accountNum
) {
}
