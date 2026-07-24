package com.kbinvestory.backend.account.domain.services;

import com.kbinvestory.backend.account.domain.constant.ConnectionStatus;
import com.kbinvestory.backend.account.domain.exception.AccountConnectionException;
import com.kbinvestory.backend.account.domain.exception.AccountErrorCode;
import com.kbinvestory.backend.account.domain.model.AccountConnection;
import com.kbinvestory.backend.account.domain.model.BrokerageProviderFixture;
import com.kbinvestory.backend.account.domain.ports.dto.BrokerAuthInfo;
import com.kbinvestory.backend.account.domain.repositories.FakeAccountConnectionRepository;
import com.kbinvestory.backend.account.domain.repositories.FakeBrokerageProviderRepository;
import com.kbinvestory.backend.account.domain.services.dto.command.CreateBrokerConnectionCommand;
import com.kbinvestory.backend.account.domain.services.dto.result.BrokerConnectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountConnectionServiceTest {

    private FakeBrokerageProviderRepository brokerageProviderRepository;
    private FakeAccountConnectionRepository accountConnectionRepository;

    @BeforeEach
    void setUp() {
        brokerageProviderRepository = new FakeBrokerageProviderRepository();
        brokerageProviderRepository.add(BrokerageProviderFixture.provider(1L, "KB", "KB증권", true));
        accountConnectionRepository = new FakeAccountConnectionRepository();
    }

    @Test
    void 인증에_성공하면_연동이_저장된다() {
        AccountConnectionService service = new AccountConnectionService(
                brokerageProviderRepository, accountConnectionRepository,
                (providerCode, loginId, password) -> new BrokerAuthInfo(true, "CONNECTED_ID_1", null));

        BrokerConnectionResult result = service.connect(new CreateBrokerConnectionCommand(100L, 1L, "myid", "mypw"));

        assertEquals(ConnectionStatus.CONNECTED, result.status());
    }

    @Test
    void 존재하지_않는_증권사면_예외가_발생한다() {
        AccountConnectionService service = new AccountConnectionService(
                brokerageProviderRepository, accountConnectionRepository,
                (providerCode, loginId, password) -> new BrokerAuthInfo(true, "CONNECTED_ID_1", null));

        AccountConnectionException exception = assertThrows(AccountConnectionException.class,
                () -> service.connect(new CreateBrokerConnectionCommand(100L, 999L, "myid", "mypw")));

        assertEquals(AccountErrorCode.PROVIDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 이미_연동된_증권사면_예외가_발생한다() {
        accountConnectionRepository.add(AccountConnection.create(100L, 1L, "EXISTING_ID"));
        AccountConnectionService service = new AccountConnectionService(
                brokerageProviderRepository, accountConnectionRepository,
                (providerCode, loginId, password) -> new BrokerAuthInfo(true, "CONNECTED_ID_1", null));

        AccountConnectionException exception = assertThrows(AccountConnectionException.class,
                () -> service.connect(new CreateBrokerConnectionCommand(100L, 1L, "myid", "mypw")));

        assertEquals(AccountErrorCode.ALREADY_CONNECTED, exception.getErrorCode());
    }

    @Test
    void 연동이_해제된_상태면_재연동을_허용한다() {
        accountConnectionRepository.add(AccountConnection.of(1L, 100L, 1L, "OLD_ID",
                ConnectionStatus.DISCONNECTED, Instant.now(), null, Instant.now(), Instant.now(), Instant.now()));
        AccountConnectionService service = new AccountConnectionService(
                brokerageProviderRepository, accountConnectionRepository,
                (providerCode, loginId, password) -> new BrokerAuthInfo(true, "NEW_ID", null));

        BrokerConnectionResult result = service.connect(new CreateBrokerConnectionCommand(100L, 1L, "myid", "mypw"));

        assertEquals(ConnectionStatus.CONNECTED, result.status());
        assertEquals(1L, result.connectionId());
    }

    @Test
    void 증권사_인증에_실패하면_예외가_발생한다() {
        AccountConnectionService service = new AccountConnectionService(
                brokerageProviderRepository, accountConnectionRepository,
                (providerCode, loginId, password) -> new BrokerAuthInfo(false, null, "비밀번호 불일치"));

        AccountConnectionException exception = assertThrows(AccountConnectionException.class,
                () -> service.connect(new CreateBrokerConnectionCommand(100L, 1L, "myid", "wrongpw")));

        assertEquals(AccountErrorCode.BROKER_AUTH_FAILED, exception.getErrorCode());
    }
}
