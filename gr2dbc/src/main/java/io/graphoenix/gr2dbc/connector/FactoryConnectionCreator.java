package io.graphoenix.gr2dbc.connector;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

public class FactoryConnectionCreator extends ConnectionCreator {

    private final ConnectionFactory connectionFactory;

    public FactoryConnectionCreator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Mono<Connection> createConnection() {
        return Mono.from(connectionFactory.create());
    }

}
