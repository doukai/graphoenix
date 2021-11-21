package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.spi.config.R2DBCConfig;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;

import java.time.Duration;

import static io.graphoenix.r2dbc.connector.connection.ConnectionFactoryCreator.FACTORY_CREATOR;

public enum ConnectionPoolCreator {

    CONNECTION_POOL_CREATOR;

    public ConnectionPool createConnectionPool(R2DBCConfig r2DBCConfig) {

        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration
                .builder(FACTORY_CREATOR.createFactory(r2DBCConfig))
                .maxIdleTime(Duration.ofMillis(r2DBCConfig.getPoolMaxIdleTime()))
                .maxSize(r2DBCConfig.getPoolMaxSize())
                .build();

        return new ConnectionPool(poolConfiguration);
    }
}
