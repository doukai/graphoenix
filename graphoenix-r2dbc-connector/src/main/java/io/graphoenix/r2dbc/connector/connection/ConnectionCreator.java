package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import static io.graphoenix.r2dbc.connector.connection.ConnectionFactoryCreator.FACTORY_CREATOR;
import static io.graphoenix.r2dbc.connector.connection.ConnectionPoolCreator.CONNECTION_POOL_CREATOR;

public class ConnectionCreator implements IConnectionCreator {

    private final ConnectionFactory connectionFactory;

    public ConnectionCreator(ConnectionConfiguration connectionConfiguration) {
        if (connectionConfiguration.isUsePool()) {
            connectionFactory = CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration);
        } else {
            connectionFactory = FACTORY_CREATOR.createFactory(connectionConfiguration);
        }
    }

    @Override
    public Mono<Connection> createConnection() {
        return Mono.from(connectionFactory.create());
    }
}
