package io.graphoenix.gr2dbc.connector;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Mono;

public abstract class ConnectionCreator {

    abstract Mono<Connection> createConnection();

    private void closeConnection(Connection connection) {
        connection.close();
    }
}
