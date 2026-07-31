package com.investory.broker.domain.services;

import com.investory.broker.domain.model.BrokerConnectionFixture;
import com.investory.broker.domain.repositories.FakeBrokerConnectionRepository;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerConnectionServiceTest {

    private FakeBrokerConnectionRepository brokerConnectionRepository;
    private BrokerConnectionService brokerConnectionService;

    @BeforeEach
    void setUp() {
        brokerConnectionRepository = new FakeBrokerConnectionRepository();
        brokerConnectionService = new BrokerConnectionService(brokerConnectionRepository);
    }

    @Test
    void 본인이_연동한_증권사_목록만_반환한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(1L, 1L, "S9990001A", "미래에셋증권(모의)"));
        brokerConnectionRepository.add(2L, BrokerConnectionFixture.connected(2L, 1L, "S9990001A", "미래에셋증권(모의)"));

        List<BrokerConnectionResult> results = brokerConnectionService.getConnections(1L);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).connectionId());
    }

    @Test
    void 연동한_증권사가_없으면_빈_목록을_반환한다() {
        List<BrokerConnectionResult> results = brokerConnectionService.getConnections(1L);

        assertTrue(results.isEmpty());
    }
}
