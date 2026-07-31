package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeBrokerConnectionRepository implements BrokerConnectionRepository {

    private record Owned(Long userId, BrokerConnection connection) {
    }

    private final List<Owned> connections = new ArrayList<>();

    public void add(Long userId, BrokerConnection connection) {
        connections.add(new Owned(userId, connection));
    }

    @Override
    public List<BrokerConnection> findAllByUserId(Long userId) {
        return connections.stream()
                .filter(owned -> owned.userId().equals(userId))
                .map(Owned::connection)
                .collect(Collectors.toList());
    }
}