package com.investory.broker.infra.port_impls;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.model.BrokerConnectionFixture;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.ports.FakeAccountDataCleanupPort;
import com.investory.broker.domain.ports.FakeBrokerFeedPort;
import com.investory.broker.domain.ports.FakeHoldingIngestionPort;
import com.investory.broker.domain.ports.FakeTradeIngestionPort;
import com.investory.broker.domain.repositories.FakeAccountSyncBatchRepository;
import com.investory.broker.domain.repositories.FakeBrokerConnectionRepository;
import com.investory.broker.domain.repositories.FakeBrokerProviderRepository;
import com.investory.broker.domain.repositories.FakeInvestmentAccountRepository;
import com.investory.broker.domain.services.BrokerAccountSyncService;
import com.investory.broker.domain.services.BrokerConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerConnectionCleanupPortImplTest {

    private static final Long USER_ID = 1L;

    private FakeBrokerConnectionRepository brokerConnectionRepository;
    private FakeInvestmentAccountRepository investmentAccountRepository;
    private BrokerConnectionCleanupPortImpl port;

    @BeforeEach
    void setUp() {
        brokerConnectionRepository = new FakeBrokerConnectionRepository();
        investmentAccountRepository = new FakeInvestmentAccountRepository();
        FakeBrokerFeedPort brokerFeedPort = new FakeBrokerFeedPort();
        BrokerAccountSyncService brokerAccountSyncService = new BrokerAccountSyncService(
                investmentAccountRepository, new FakeTradeIngestionPort(), new FakeHoldingIngestionPort(), brokerFeedPort);
        BrokerConnectionService brokerConnectionService = new BrokerConnectionService(
                brokerConnectionRepository,
                new FakeBrokerProviderRepository(),
                new FakeAccountSyncBatchRepository(),
                investmentAccountRepository,
                brokerFeedPort,
                brokerAccountSyncService,
                new FakeAccountDataCleanupPort()
        );
        port = new BrokerConnectionCleanupPortImpl(brokerConnectionRepository, brokerConnectionService);
    }

    @Test
    void 사용자의_모든_연결을_해지한다() {
        brokerConnectionRepository.add(USER_ID, BrokerConnectionFixture.connected(10L, 1L, "S9990001A", "미래에셋증권(모의)"));
        brokerConnectionRepository.add(USER_ID, BrokerConnectionFixture.connected(20L, 2L, "S9990002A", "키움증권(모의)"));
        investmentAccountRepository.add(USER_ID, InvestmentAccount.of(
                100L, 10L, "111-111", "111-***-111", "계좌1", AccountType.STOCK, "KRW"));

        port.disconnectAllConnections(USER_ID);

        assertTrue(investmentAccountRepository.findByConnectionId(10L).isEmpty());
    }

    @Test
    void 이미_해지된_연결은_건너뛴다() {
        brokerConnectionRepository.add(USER_ID, BrokerConnectionFixture.connection(
                10L, 1L, "S9990001A", "미래에셋증권(모의)", ConnectionStatus.DISCONNECTED,
                Instant.parse("2026-07-29T13:40:00Z"), null, 0));

        // 예외 없이 끝나면 성공 — 이미 DISCONNECTED인 연결에 다시 disconnectConnection을 걸지 않는다는 뜻
        port.disconnectAllConnections(USER_ID);

        assertEquals(ConnectionStatus.DISCONNECTED,
                brokerConnectionRepository.findByIdAndUserId(10L, USER_ID).orElseThrow().getConnectionStatus());
    }
}
