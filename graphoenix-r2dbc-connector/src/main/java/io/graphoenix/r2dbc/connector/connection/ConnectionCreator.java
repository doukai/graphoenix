package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.spi.config.R2DBCConfig;
import io.r2dbc.spi.Connection;
import reactor.core.publisher.Mono;

import javax.inject.Inject;

public class ConnectionCreator {

    private final ConnectionFactoryCreator connectionFactoryCreator;
    private final ConnectionPoolCreator connectionPoolCreator;
    private final R2DBCConfig r2DBCConfig;

    @Inject
    public ConnectionCreator(ConnectionFactoryCreator connectionFactoryCreator, ConnectionPoolCreator connectionPoolCreator, R2DBCConfig r2DBCConfig) {
        this.connectionFactoryCreator = connectionFactoryCreator;
        this.connectionPoolCreator = connectionPoolCreator;
        this.r2DBCConfig = r2DBCConfig;
    }

    public Mono<Connection> createConnection() {
        if (r2DBCConfig.getUsePool()) {
            return Mono.from(connectionPoolCreator.createConnectionPool().create());
        } else {
            return Mono.from(connectionFactoryCreator.createFactory().create());
        }
    }
}
