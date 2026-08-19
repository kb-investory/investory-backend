package com.investory.broker.domain.services;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.exception.BrokerErrorCode;
import com.investory.broker.domain.exception.BrokerException;
import com.investory.broker.domain.model.AccountSyncBatch;
import com.investory.broker.domain.model.BrokerConnectionFixture;
import com.investory.broker.domain.model.BrokerProviderFixture;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.ports.FakeAccountDataCleanupPort;
import com.investory.broker.domain.ports.FakeHoldingIngestionPort;
import com.investory.broker.domain.ports.FakeTradeIngestionPort;
import com.investory.broker.domain.repositories.FakeAccountSyncBatchRepository;
import com.investory.broker.domain.repositories.FakeBrokerConnectionRepository;
import com.investory.broker.domain.repositories.FakeBrokerProviderRepository;
import com.investory.broker.domain.repositories.FakeInvestmentAccountRepository;
import com.investory.broker.domain.services.dto.command.CreateBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.command.DisconnectBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.command.SyncBrokerConnectionCommand;
import com.investory.broker.domain.services.dto.query.GetBrokerConnectionDetailQuery;
import com.investory.broker.domain.services.dto.result.BrokerConnectionDetailResult;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.CreateBrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.DisconnectBrokerConnectionResult;
import com.investory.broker.domain.services.dto.result.SyncConnectionResult;
import com.investory.broker.domain.ports.FakeBrokerFeedPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private FakeAccountDataCleanupPort accountDataCleanupPort;
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
        accountDataCleanupPort = new FakeAccountDataCleanupPort();
        BrokerAccountSyncService brokerAccountSyncService = new BrokerAccountSyncService(
                investmentAccountRepository, tradeIngestionPort, holdingIngestionPort, brokerFeedPort);
        brokerConnectionService = new BrokerConnectionService(
                brokerConnectionRepository,
                brokerProviderRepository,
                accountSyncBatchRepository,
                investmentAccountRepository,
                brokerFeedPort,
                brokerAccountSyncService,
                accountDataCleanupPort
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
    void 해지된_연결은_목록에_나오지_않는다() {
        // 해지 후 같은 증권사에 재연동하면 옛 DISCONNECTED 행과 새 CONNECTED 행이 함께 남는데,
        // 목록에는 재연동한 것만 보여야 한다.
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connection(
                10L, 1L, "S9990001A", "미래에셋증권(모의)", ConnectionStatus.DISCONNECTED,
                Instant.parse("2026-07-29T13:40:00Z"), null, 0));
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(20L, 1L, "S9990001A", "미래에셋증권(모의)"));

        List<BrokerConnectionResult> results = brokerConnectionService.getConnections(1L);

        assertEquals(1, results.size());
        assertEquals(20L, results.get(0).connectionId());
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
    void 로그인한_계정의_실제_소속과_선택한_증권사가_다르면_예외가_발생하고_커넥션이_생성되지_않는다() {
        brokerProviderRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        brokerFeedPort.willLoginAs("KIWOOM", "키움증권(모의)");
        CreateBrokerConnectionCommand command = new CreateBrokerConnectionCommand(1L, 1L, "demo1", "1234");

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.createConnection(command));

        assertEquals(BrokerErrorCode.ORG_MISMATCH, exception.getErrorCode());
        assertTrue(brokerConnectionService.getConnections(1L).isEmpty());
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
        assertEquals("목 서버 응답 오류", result.syncResult().errorMessage());
        assertEquals(0, result.syncResult().accountCount());
    }

    @Test
    void 여러_계좌_중_하나라도_실패하면_전체_동기화가_실패로_처리된다() {
        brokerProviderRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        brokerFeedPort.willReturnAccounts(List.of(
                new com.investory.broker.domain.ports.dto.RawAccountRecord("111-111", "계좌1", "101", "KRW"),
                new com.investory.broker.domain.ports.dto.RawAccountRecord("222-222", "계좌2", "101", "KRW")
        ));
        brokerFeedPort.willFailAccountWith("222-222", new RuntimeException("두번째 계좌 조회 실패"));
        CreateBrokerConnectionCommand command = new CreateBrokerConnectionCommand(1L, 1L, "demo1", "1234");

        CreateBrokerConnectionResult result = brokerConnectionService.createConnection(command);

        assertEquals(ConnectionStatus.CONNECTED, result.connectionStatus());
        assertEquals(SyncStatus.FAILED, result.syncResult().syncStatus());
        assertEquals(0, result.syncResult().accountCount());
    }

    @Test
    void 연결_상세_조회시_최근_동기화_이력을_함께_반환한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        accountSyncBatchRepository.add(AccountSyncBatch.of(
                101L, 15L, SyncStatus.SUCCESS, Instant.parse("2026-07-29T14:45:00Z"), Instant.parse("2026-07-29T14:45:03Z"), null));

        BrokerConnectionDetailResult result = brokerConnectionService.getConnectionDetail(
                new GetBrokerConnectionDetailQuery(1L, 15L));

        assertEquals(15L, result.connectionId());
        assertEquals(SyncStatus.SUCCESS, result.latestSync().syncStatus());
        assertEquals(101L, result.latestSync().syncBatchId());
    }

    @Test
    void 동기화_이력이_없으면_latestSync는_null이다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));

        BrokerConnectionDetailResult result = brokerConnectionService.getConnectionDetail(
                new GetBrokerConnectionDetailQuery(1L, 15L));

        assertNull(result.latestSync());
    }

    @Test
    void 본인_소유가_아닌_연결을_조회하면_예외가_발생한다() {
        brokerConnectionRepository.add(2L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        GetBrokerConnectionDetailQuery query = new GetBrokerConnectionDetailQuery(1L, 15L);

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.getConnectionDetail(query));

        assertEquals(BrokerErrorCode.CONNECTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 재동기화에_성공하면_SUCCESS와_집계_결과를_반환한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        brokerConnectionRepository.addMockProfileCode(15L, "demo1");

        SyncConnectionResult result = brokerConnectionService.syncConnection(new SyncBrokerConnectionCommand(1L, 15L));

        assertEquals(15L, result.connectionId());
        assertEquals(SyncStatus.SUCCESS, result.syncStatus());
        assertEquals(0, result.accountCount());
    }

    @Test
    void 존재하지_않는_연결을_재동기화하면_예외가_발생한다() {
        SyncBrokerConnectionCommand command = new SyncBrokerConnectionCommand(1L, 999L);

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.syncConnection(command));

        assertEquals(BrokerErrorCode.CONNECTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 재동기화_도중_오류가_나면_배치는_FAILED로_기록되고_예외는_던지지_않는다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        brokerConnectionRepository.addMockProfileCode(15L, "demo1");
        brokerFeedPort.willFailSyncWith(new RuntimeException("목 서버 응답 오류"));

        SyncConnectionResult result = brokerConnectionService.syncConnection(new SyncBrokerConnectionCommand(1L, 15L));

        assertEquals(SyncStatus.FAILED, result.syncStatus());
        assertEquals(0, result.accountCount());
        assertEquals("목 서버 응답 오류", result.errorMessage());
    }

    @Test
    void 연동을_해지하면_계좌별로_ledger_데이터_삭제를_위임하고_계좌를_지우고_상태를_전이한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        investmentAccountRepository.add(1L, InvestmentAccount.of(
                2000L, 15L, "111-111", "111-***-111", "계좌1", AccountType.STOCK, "KRW"));
        investmentAccountRepository.add(1L, InvestmentAccount.of(
                2001L, 15L, "222-222", "222-***-222", "계좌2", AccountType.STOCK, "KRW"));

        DisconnectBrokerConnectionResult result = brokerConnectionService.disconnectConnection(
                new DisconnectBrokerConnectionCommand(1L, 15L));

        assertEquals(ConnectionStatus.DISCONNECTED, result.connectionStatus());
        assertEquals(List.of(2000L, 2001L), accountDataCleanupPort.deletedAccountIds());
        assertTrue(investmentAccountRepository.findByConnectionId(15L).isEmpty());
    }

    @Test
    void 존재하지_않는_연결을_해지하면_예외가_발생한다() {
        DisconnectBrokerConnectionCommand command = new DisconnectBrokerConnectionCommand(1L, 999L);

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.disconnectConnection(command));

        assertEquals(BrokerErrorCode.CONNECTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 본인_소유가_아닌_연결을_해지하려_하면_예외가_발생한다() {
        brokerConnectionRepository.add(2L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        DisconnectBrokerConnectionCommand command = new DisconnectBrokerConnectionCommand(1L, 15L);

        BrokerException exception = assertThrows(BrokerException.class, () -> brokerConnectionService.disconnectConnection(command));

        assertEquals(BrokerErrorCode.CONNECTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 이미_해지된_연결을_다시_해지해도_데이터_삭제가_재실행되지_않는다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connection(
                15L, 1L, "S9990001A", "미래에셋증권(모의)", ConnectionStatus.DISCONNECTED,
                Instant.parse("2026-07-29T13:40:00Z"), null, 0));

        DisconnectBrokerConnectionResult result = brokerConnectionService.disconnectConnection(
                new DisconnectBrokerConnectionCommand(1L, 15L));

        assertEquals(ConnectionStatus.DISCONNECTED, result.connectionStatus());
        assertTrue(accountDataCleanupPort.deletedAccountIds().isEmpty());
    }
}
