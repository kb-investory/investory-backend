package com.investory.broker.infra.schedulers;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.model.AccountSyncBatch;
import com.investory.broker.domain.model.BrokerConnectionFixture;
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

class BrokerAccountSyncSchedulerTest {

    private FakeBrokerConnectionRepository brokerConnectionRepository;
    private FakeAccountSyncBatchRepository accountSyncBatchRepository;
    private FakeBrokerFeedPort brokerFeedPort;
    private BrokerAccountSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        brokerConnectionRepository = new FakeBrokerConnectionRepository();
        accountSyncBatchRepository = new FakeAccountSyncBatchRepository();
        FakeInvestmentAccountRepository investmentAccountRepository = new FakeInvestmentAccountRepository();
        brokerFeedPort = new FakeBrokerFeedPort();
        BrokerAccountSyncService brokerAccountSyncService = new BrokerAccountSyncService(
                investmentAccountRepository, new FakeTradeIngestionPort(), new FakeHoldingIngestionPort(), brokerFeedPort);
        BrokerConnectionService brokerConnectionService = new BrokerConnectionService(
                brokerConnectionRepository, new FakeBrokerProviderRepository(), accountSyncBatchRepository, investmentAccountRepository,
                brokerFeedPort, brokerAccountSyncService, new FakeAccountDataCleanupPort());
        scheduler = new BrokerAccountSyncScheduler(brokerConnectionRepository, brokerConnectionService);
    }

    @Test
    void 활성_연결_전체를_순회하며_동기화한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(10L, 1L, "S9990001A", "미래에셋증권(모의)"));
        brokerConnectionRepository.add(2L, BrokerConnectionFixture.connected(20L, 1L, "S9990001A", "미래에셋증권(모의)"));
        brokerConnectionRepository.addMockProfileCode(10L, "demo1");
        brokerConnectionRepository.addMockProfileCode(20L, "demo2");

        scheduler.syncAllActiveConnections();

        assertEquals(SyncStatus.SUCCESS, latestStatus(10L));
        assertEquals(SyncStatus.SUCCESS, latestStatus(20L));
    }

    @Test
    void 해지된_연결은_동기화_대상에서_제외된다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connection(
                30L, 1L, "S9990001A", "미래에셋증권(모의)", ConnectionStatus.DISCONNECTED,
                Instant.parse("2026-07-29T13:40:00Z"), null, 0));

        scheduler.syncAllActiveConnections();

        assertTrue(accountSyncBatchRepository.findLatestByConnectionId(30L).isEmpty());
    }

    @Test
    void 이미_진행_중인_연결은_건너뛰고_나머지는_계속_동기화한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(10L, 1L, "S9990001A", "미래에셋증권(모의)"));
        brokerConnectionRepository.add(2L, BrokerConnectionFixture.connected(20L, 1L, "S9990001A", "미래에셋증권(모의)"));
        accountSyncBatchRepository.add(AccountSyncBatch.of(500L, 10L, SyncStatus.REQUESTED, Instant.now(), null, null));

        scheduler.syncAllActiveConnections();

        // 10L은 이미 진행 중이라 새 배치가 안 생기고 REQUESTED 그대로, 20L은 정상적으로 동기화된다
        assertEquals(SyncStatus.REQUESTED, latestStatus(10L));
        assertEquals(SyncStatus.SUCCESS, latestStatus(20L));
    }

    private SyncStatus latestStatus(Long connectionId) {
        return accountSyncBatchRepository.findLatestByConnectionId(connectionId)
                .map(AccountSyncBatch::getSyncStatus)
                .orElseThrow();
    }
}
