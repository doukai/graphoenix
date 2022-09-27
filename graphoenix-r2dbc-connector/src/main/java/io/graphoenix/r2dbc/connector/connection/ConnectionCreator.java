package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.r2dbc.connector.config.R2DBCConfig;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@ApplicationScoped
public class ConnectionCreator {

    private final ConnectionFactory connectionFactory;

    @Inject
    public ConnectionCreator(ConnectionFactoryCreator connectionFactoryCreator, ConnectionPoolCreator connectionPoolCreator, R2DBCConfig r2DBCConfig) {
        if (r2DBCConfig.getUsePool()) {
            this.connectionFactory = connectionPoolCreator.getConnectionPool();
        } else {
            this.connectionFactory = connectionFactoryCreator.createFactory();
        }
    }

    public Mono<Connection> createConnection() {
        return Mono.from(connectionFactory.create());
    }
}
