package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerProvider;

import java.util.List;
import java.util.Optional;

public interface BrokerProviderRepository {
    List<BrokerProvider> findAllActive();

    Optional<BrokerProvider> findById(Long brokerId);

    // brokerCode가 이미 있으면 이름 갱신 + 활성화, 없으면 새로 만든다.
    void upsertByCode(String brokerCode, String brokerName);

    // brokerCodes에 없는 기존 provider는 비활성화한다 (하드 삭제 대신 — broker_connections FK 보존).
    void deactivateExcept(List<String> brokerCodes);
}