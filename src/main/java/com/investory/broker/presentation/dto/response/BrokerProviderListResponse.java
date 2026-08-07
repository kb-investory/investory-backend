package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.services.dto.result.BrokerProviderResult;

import java.util.List;
import java.util.stream.Collectors;

public record BrokerProviderListResponse(
    List<BrokerProviderResponse> providers
) {
    public static BrokerProviderListResponse from(List<BrokerProviderResult> results) {
        return new BrokerProviderListResponse(
                results.stream().map(BrokerProviderResponse::from).collect(Collectors.toList())
        );
    }
}