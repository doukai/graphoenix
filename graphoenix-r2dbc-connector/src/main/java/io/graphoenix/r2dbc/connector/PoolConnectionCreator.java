package io.graphoenix.r2dbc.connector;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.Connection;
import reactor.core.publisher.Mono;

public class PoolConnectionCreator implements ConnectionCreator {

    private final ConnectionPool connectionPool;

    public PoolConnectionCreator(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public Mono<Connection> createConnection() {
        return connectionPool.create();
    }
}
