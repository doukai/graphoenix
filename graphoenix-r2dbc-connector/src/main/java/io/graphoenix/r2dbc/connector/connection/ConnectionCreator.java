package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.spi.config.R2DBCConfig;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import static io.graphoenix.r2dbc.connector.connection.ConnectionFactoryCreator.FACTORY_CREATOR;
import static io.graphoenix.r2dbc.connector.connection.ConnectionPoolCreator.CONNECTION_POOL_CREATOR;

public class ConnectionCreator implements IConnectionCreator {

    private final ConnectionFactory connectionFactory;

    public ConnectionCreator(R2DBCConfig r2DBCConfig) {
        if (r2DBCConfig.isUsePool()) {
            connectionFactory = CONNECTION_POOL_CREATOR.createConnectionPool(r2DBCConfig);
        } else {
            connectionFactory = FACTORY_CREATOR.createFactory(r2DBCConfig);
        }
    }

    @Override
    public Mono<Connection> createConnection() {
        return Mono.from(connectionFactory.create());
    }
}
