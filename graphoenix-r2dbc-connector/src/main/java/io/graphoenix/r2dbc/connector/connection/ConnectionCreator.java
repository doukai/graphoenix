package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.r2dbc.connector.config.R2DBCConfig;
import io.r2dbc.spi.Connection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@ApplicationScoped
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
            return connectionPoolCreator.createConnectionPool().create();
        } else {
            return Mono.from(connectionFactoryCreator.createFactory().create());
        }
    }
}
