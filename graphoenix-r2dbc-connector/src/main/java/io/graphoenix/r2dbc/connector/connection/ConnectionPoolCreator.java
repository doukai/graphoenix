package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;

import java.time.Duration;

import static io.graphoenix.r2dbc.connector.connection.ConnectionFactoryCreator.FACTORY_CREATOR;

public enum ConnectionPoolCreator {

    CONNECTION_POOL_CREATOR;

    public ConnectionPool createConnectionPool(ConnectionConfiguration connectionConfiguration) {

        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration
                .builder(FACTORY_CREATOR.createFactory(connectionConfiguration))
                .maxIdleTime(Duration.ofMillis(connectionConfiguration.getPoolMaxIdleTime()))
                .maxSize(connectionConfiguration.getPoolMaxSize())
                .build();

        return new ConnectionPool(poolConfiguration);
    }
}
