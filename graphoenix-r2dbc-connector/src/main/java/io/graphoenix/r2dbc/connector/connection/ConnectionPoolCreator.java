package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.r2dbc.connector.config.R2DBCConfig;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;

import javax.inject.Inject;
import java.time.Duration;

public class ConnectionPoolCreator {

    private final ConnectionFactoryCreator connectionFactoryCreator;
    private final R2DBCConfig r2DBCConfig;

    @Inject
    public ConnectionPoolCreator(ConnectionFactoryCreator connectionFactoryCreator, R2DBCConfig r2DBCConfig) {
        this.connectionFactoryCreator = connectionFactoryCreator;
        this.r2DBCConfig = r2DBCConfig;
    }

    public ConnectionPool createConnectionPool() {

        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration
                .builder(connectionFactoryCreator.createFactory())
                .maxIdleTime(Duration.ofMillis(r2DBCConfig.getPoolMaxIdleTime()))
                .maxSize(r2DBCConfig.getPoolMaxSize())
                .build();

        return new ConnectionPool(poolConfiguration);
    }
}
