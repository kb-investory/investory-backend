package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerConnection;

import java.util.List;

public interface BrokerConnectionRepository {
    List<BrokerConnection> findAllByUserId(Long userId);
}