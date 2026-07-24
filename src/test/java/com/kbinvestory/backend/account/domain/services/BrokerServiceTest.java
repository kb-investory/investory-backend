package com.kbinvestory.backend.account.domain.services;

import com.kbinvestory.backend.account.domain.model.BrokerageProviderFixture;
import com.kbinvestory.backend.account.domain.repositories.FakeBrokerageProviderRepository;
import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;
import com.kbinvestory.backend.account.domain.services.dto.result.BrokerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerServiceTest {

    private FakeBrokerageProviderRepository brokerageProviderRepository;
    private BrokerService brokerService;

    @BeforeEach
    void setUp() {
        brokerageProviderRepository = new FakeBrokerageProviderRepository();
        brokerService = new BrokerService(brokerageProviderRepository);
    }

    @Test
    void 검색어_없이_조회하면_활성_증권사를_모두_반환한다() {
        brokerageProviderRepository.add(
                BrokerageProviderFixture.provider("KB", "KB증권", true),
                BrokerageProviderFixture.provider("NH", "NH투자증권", true)
        );

        List<BrokerResult> results = brokerService.getBrokers(new GetBrokersQuery(null));

        assertEquals(2, results.size());
    }

    @Test
    void 비활성_증권사는_결과에서_제외한다() {
        brokerageProviderRepository.add(
                BrokerageProviderFixture.provider("KB", "KB증권", true),
                BrokerageProviderFixture.provider("OLD", "폐업증권", false)
        );

        List<BrokerResult> results = brokerService.getBrokers(new GetBrokersQuery(null));

        assertEquals(1, results.size());
        assertEquals("KB", results.get(0).code());
    }

    @Test
    void 코드와_이름_모두에서_키워드를_검색한다() {
        brokerageProviderRepository.add(
                BrokerageProviderFixture.provider("KB", "KB증권", true),
                BrokerageProviderFixture.provider("NH", "NH투자증권", true)
        );

        List<BrokerResult> results = brokerService.getBrokers(new GetBrokersQuery("투자"));

        assertEquals(1, results.size());
        assertEquals("NH", results.get(0).code());
    }

    @Test
    void 조건에_맞는_증권사가_없으면_빈_결과를_반환한다() {
        brokerageProviderRepository.add(
                BrokerageProviderFixture.provider("KB", "KB증권", true)
        );

        List<BrokerResult> results = brokerService.getBrokers(new GetBrokersQuery("없는증권사"));

        assertTrue(results.isEmpty());
    }
}