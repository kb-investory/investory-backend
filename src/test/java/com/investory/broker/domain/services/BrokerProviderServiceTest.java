package com.investory.broker.domain.services;

import com.investory.broker.domain.model.BrokerProviderFixture;
import com.investory.broker.domain.repositories.FakeBrokerProviderRepository;
import com.investory.broker.domain.services.dto.result.BrokerProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerProviderServiceTest {

    private FakeBrokerProviderRepository brokerProviderRepository;
    private BrokerProviderService brokerProviderService;

    @BeforeEach
    void setUp() {
        brokerProviderRepository = new FakeBrokerProviderRepository();
        brokerProviderService = new BrokerProviderService(brokerProviderRepository);
    }

    @Test
    void 활성_증권사_목록을_반환한다() {
        brokerProviderRepository.add(
                BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)")
        );

        List<BrokerProviderResult> results = brokerProviderService.getProviders();

        assertEquals(1, results.size());
        assertEquals("S9990001A", results.get(0).brokerCode());
        assertEquals("미래에셋증권(모의)", results.get(0).brokerName());
    }

    @Test
    void 증권사가_없으면_빈_목록을_반환한다() {
        List<BrokerProviderResult> results = brokerProviderService.getProviders();

        assertTrue(results.isEmpty());
    }

    @Test
    void 비활성_증권사는_제외한다() {
        brokerProviderRepository.add(
                BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)", true),
                BrokerProviderFixture.provider(2L, "KIWOOM", "키움증권", false)
        );

        List<BrokerProviderResult> results = brokerProviderService.getProviders();

        assertEquals(1, results.size());
        assertEquals("S9990001A", results.get(0).brokerCode());
    }
}