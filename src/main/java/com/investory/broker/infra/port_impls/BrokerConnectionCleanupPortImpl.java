package com.investory.broker.infra.port_impls;

import com.investory.auth.domain.ports.BrokerConnectionCleanupPort;
import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.services.BrokerConnectionService;
import com.investory.broker.domain.services.dto.command.DisconnectBrokerConnectionCommand;
import org.springframework.stereotype.Component;

// auth.domain.ports를 참조하는 유일한 지점 — 받는 즉시 broker 자신의 서비스 호출로 위임한다(§5).
// 이미 존재하는 disconnectConnection()을 사용자의 모든 연결에 반복 호출해 재사용한다.
@Component
public class BrokerConnectionCleanupPortImpl implements BrokerConnectionCleanupPort {

    private final BrokerConnectionRepository brokerConnectionRepository;
    private final BrokerConnectionService brokerConnectionService;

    public BrokerConnectionCleanupPortImpl(BrokerConnectionRepository brokerConnectionRepository,
                                            BrokerConnectionService brokerConnectionService) {
        this.brokerConnectionRepository = brokerConnectionRepository;
        this.brokerConnectionService = brokerConnectionService;
    }

    @Override
    public void disconnectAllConnections(Long userId) {
        for (BrokerConnection connection : brokerConnectionRepository.findAllByUserId(userId)) {
            if (connection.getConnectionStatus() != ConnectionStatus.DISCONNECTED) {
                brokerConnectionService.disconnectConnection(
                        new DisconnectBrokerConnectionCommand(userId, connection.getConnectionId()));
            }
        }
    }
}
