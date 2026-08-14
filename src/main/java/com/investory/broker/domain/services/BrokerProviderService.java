package com.investory.broker.domain.services;

import com.investory.broker.domain.ports.BrokerFeedPort;
import com.investory.broker.domain.ports.dto.RawOrganizationRecord;
import com.investory.broker.domain.repositories.BrokerProviderRepository;
import com.investory.broker.domain.services.dto.result.BrokerProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrokerProviderService {

    private static final Logger log = LoggerFactory.getLogger(BrokerProviderService.class);

    private final BrokerProviderRepository brokerProviderRepository;
    private final BrokerFeedPort brokerFeedPort;

    public BrokerProviderService(BrokerProviderRepository brokerProviderRepository, BrokerFeedPort brokerFeedPort) {
        this.brokerProviderRepository = brokerProviderRepository;
        this.brokerFeedPort = brokerFeedPort;
    }

    public List<BrokerProviderResult> getProviders() {
        return brokerProviderRepository.findAllActive().stream()
                .map(BrokerProviderResult::from)
                .collect(Collectors.toList());
    }

    // 목 서버가 알고 있는 기관(org) 목록을 broker_providers에 반영한다: 신규는 추가,
    // 기존은 이름 갱신, 더 이상 내려오지 않는 코드는 비활성화. 목 서버 응답이 비어 있으면
    // (일시적 장애 등으로) 전체를 비활성화해버리는 사고를 막기 위해 아무것도 하지 않는다.
    @Transactional
    public void syncProviders() {
        List<RawOrganizationRecord> organizations = brokerFeedPort.fetchOrganizations();
        if (organizations.isEmpty()) {
            log.warn("목 서버로부터 받은 기관 목록이 비어 있어 broker_providers 동기화를 건너뜁니다.");
            return;
        }

        for (RawOrganizationRecord organization : organizations) {
            brokerProviderRepository.upsertByCode(organization.orgCode(), organization.orgName());
        }

        List<String> currentBrokerCodes = organizations.stream()
                .map(RawOrganizationRecord::orgCode)
                .collect(Collectors.toList());
        brokerProviderRepository.deactivateExcept(currentBrokerCodes);
    }
}