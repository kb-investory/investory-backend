package com.investory.broker.domain.services;

import com.investory.broker.domain.model.BrokerProviderFixture;
import com.investory.broker.domain.ports.FakeBrokerFeedPort;
import com.investory.broker.domain.ports.dto.RawOrganizationRecord;
import com.investory.broker.domain.repositories.FakeBrokerProviderRepository;
import com.investory.broker.domain.services.dto.result.BrokerProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerProviderServiceTest {

    private FakeBrokerProviderRepository brokerProviderRepository;
    private FakeBrokerFeedPort brokerFeedPort;
    private BrokerProviderService brokerProviderService;

    @BeforeEach
    void setUp() {
        brokerProviderRepository = new FakeBrokerProviderRepository();
        brokerFeedPort = new FakeBrokerFeedPort();
        brokerProviderService = new BrokerProviderService(brokerProviderRepository, brokerFeedPort);
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

    @Test
    void 목_서버의_신규_기관을_추가한다() {
        brokerFeedPort.willReturnOrganizations(List.of(
                new RawOrganizationRecord("S9990001A", "미래에셋증권(모의)"),
                new RawOrganizationRecord("KIWOOM", "키움증권(모의)")
        ));

        brokerProviderService.syncProviders();

        List<BrokerProviderResult> results = brokerProviderService.getProviders();
        assertEquals(2, results.size());
    }

    @Test
    void 기존_기관의_이름이_바뀌면_갱신한다() {
        brokerProviderRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "옛날이름"));
        brokerFeedPort.willReturnOrganizations(List.of(
                new RawOrganizationRecord("S9990001A", "미래에셋증권(모의)")
        ));

        brokerProviderService.syncProviders();

        List<BrokerProviderResult> results = brokerProviderService.getProviders();
        assertEquals(1, results.size());
        assertEquals("미래에셋증권(모의)", results.get(0).brokerName());
    }

    @Test
    void 목_서버_응답에서_사라진_기관은_비활성화된다() {
        brokerProviderRepository.add(
                BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"),
                BrokerProviderFixture.provider(2L, "KIWOOM", "키움증권(모의)")
        );
        brokerFeedPort.willReturnOrganizations(List.of(
                new RawOrganizationRecord("S9990001A", "미래에셋증권(모의)")
        ));

        brokerProviderService.syncProviders();

        List<BrokerProviderResult> results = brokerProviderService.getProviders();
        assertEquals(1, results.size());
        assertEquals("S9990001A", results.get(0).brokerCode());
    }

    @Test
    void 목_서버_응답이_비어있으면_기존_목록을_그대로_둔다() {
        brokerProviderRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        brokerFeedPort.willReturnOrganizations(List.of());

        brokerProviderService.syncProviders();

        List<BrokerProviderResult> results = brokerProviderService.getProviders();
        assertEquals(1, results.size());
        assertFalse(results.isEmpty());
    }
}