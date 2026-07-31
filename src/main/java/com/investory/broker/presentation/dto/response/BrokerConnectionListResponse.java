package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;

import java.util.List;
import java.util.stream.Collectors;

public record BrokerConnectionListResponse(
    List<BrokerConnectionResponse> connections
) {
    public static BrokerConnectionListResponse from(List<BrokerConnectionResult> results) {
        return new BrokerConnectionListResponse(
                results.stream().map(BrokerConnectionResponse::from).collect(Collectors.toList())
        );
    }
}