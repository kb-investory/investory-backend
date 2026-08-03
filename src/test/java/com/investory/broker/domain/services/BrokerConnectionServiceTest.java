package com.investory.broker.domain.services;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.exception.BrokerErrorCode;
import com.investory.broker.domain.exception.BrokerException;
import com.investory.broker.domain.model.BrokerConnectionFixture;
import com.investory.broker.domain.model.BrokerProviderFixture;
import com.investory.broker.domain.ports.FakeHoldingIngestionPort;
import com.investory.broker.domain.ports.FakeTradeIngestionPort;
import com.investory.broker.domain.repositories.FakeAccountSyncBatchRepository;
import com.investory.broker.domain.repositories.FakeBrokerConnectionRepository;
import com.investory.broker.domain.repositories.FakeBrokerProviderRepository;
import com.investory.broker.domain.repositories.FakeInvestmentAccountRepository;
import com.investory.broker.domain.services.dto.command.CreateBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.CreateBrokerConnectionResult;
import com.investory.broker.domain.ports.FakeBrokerFeedPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerConnectionServiceTest {

    private FakeBrokerConnectionRepository brokerConnectionRepository;
    private FakeBrokerProviderRepository brokerProviderRepository;
    private FakeInvestmentAccountRepository investmentAccountRepository;
    private FakeAccountSyncBatchRepository accountSyncBatchRepository;
    private FakeTradeIngestionPort tradeIngestionPort;
    private FakeHoldingIngestionPort holdingIngestionPort;
    private FakeBrokerFeedPort brokerFeedPort;
    private BrokerConnectionService brokerConnectionService;

    @BeforeEach
    void setUp() {
        brokerConnectionRepository = new FakeBrokerConnectionRepository();
        brokerProviderRepository = new FakeBrokerProviderRepository();
        investmentAccountRepository = new FakeInvestmentAccountRepository();
        accountSyncBatchRepository = new FakeAccountSyncBatchRepository();
        tradeIngestionPort = new FakeTradeIngestionPort();
        holdingIngestionPort = new FakeHoldingIngestionPort();
        brokerFeedPort = new FakeBrokerFeedPort();
        brokerConnectionService = new BrokerConnectionService(
                brokerConnectionRepository,
                brokerProviderRepository,
                investmentAccountRepository,
                accountSyncBatchRepository,
                tradeIngestionPort,
                holdingIngestionPort,
                brokerFeedPort
        );
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

    @Test
    void 증권사_연동에_성공하면_CONNECTED_상태와_동기화_결과를_반환한다() {
        brokerProviderRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));

        CreateBrokerConnectionResult result = brokerConnectionService.createConnection(
                new CreateBrokerConnectionCommand(1L, 1L, "demo1", "1234"));

        assertEquals(ConnectionStatus.CONNECTED, result.connectionStatus());
        assertEquals("S9990001A", result.brokerCode());
        assertEquals(SyncStatus.SUCCESS, result.syncResult().syncStatus());
        assertEquals(0, result.syncResult().accountCount());
    }

    @Test
    void 존재하지_않는_브로커면_예외가_발생한다() {
        CreateBrokerConnectionCommand command = new CreateBrokerConnectionCommand(1L, 999L, "demo1", "1234");

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.createConnection(command));

        assertEquals(BrokerErrorCode.PROVIDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 이미_연동된_증권사면_예외가_발생한다() {
        brokerProviderRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(10L, 1L, "S9990001A", "미래에셋증권(모의)"));
        CreateBrokerConnectionCommand command = new CreateBrokerConnectionCommand(1L, 1L, "demo1", "1234");

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.createConnection(command));

        assertEquals(BrokerErrorCode.ALREADY_CONNECTED, exception.getErrorCode());
    }

    @Test
    void 목_서버_인증에_실패하면_예외가_발생한다() {
        brokerProviderRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        brokerFeedPort.willFailLoginWithUnauthorized();
        CreateBrokerConnectionCommand command = new CreateBrokerConnectionCommand(1L, 1L, "demo1", "wrong-password");

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.createConnection(command));

        assertEquals(BrokerErrorCode.BROKER_AUTH_FAILED, exception.getErrorCode());
    }

    @Test
    void 입력값이_유효하지_않으면_예외가_발생한다() {
        CreateBrokerConnectionCommand command = new CreateBrokerConnectionCommand(1L, null, "", "1234");

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.createConnection(command));

        assertEquals(BrokerErrorCode.INVALID_CONNECTION_DATA, exception.getErrorCode());
    }

    @Test
    void 동기화_도중_오류가_나면_연결은_유지되고_FAILED_상태를_반환한다() {
        brokerProviderRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        brokerFeedPort.willFailSyncWith(new RuntimeException("목 서버 응답 오류"));
        CreateBrokerConnectionCommand command = new CreateBrokerConnectionCommand(1L, 1L, "demo1", "1234");

        CreateBrokerConnectionResult result = brokerConnectionService.createConnection(command);

        assertEquals(ConnectionStatus.CONNECTED, result.connectionStatus());
        assertEquals(SyncStatus.FAILED, result.syncResult().syncStatus());
        assertEquals(0, result.syncResult().accountCount());
    }
}
