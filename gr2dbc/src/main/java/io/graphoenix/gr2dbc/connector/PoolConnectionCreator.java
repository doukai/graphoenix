package io.graphoenix.gr2dbc.connector;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.Connection;
import reactor.core.publisher.Mono;

public class PoolConnectionCreator extends ConnectionCreator {

    private final ConnectionPool connectionPool;

    public PoolConnectionCreator(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public Mono<Connection> createConnection() {
        return connectionPool.create();
    }
}
