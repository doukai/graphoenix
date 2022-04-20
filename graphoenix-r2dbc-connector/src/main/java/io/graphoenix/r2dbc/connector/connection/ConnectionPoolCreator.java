package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.r2dbc.connector.config.R2DBCConfig;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;

@ApplicationScoped
public class ConnectionPoolCreator {

    private final ConnectionFactoryCreator connectionFactoryCreator;
    private final ConnectionPool connectionPool;

    @Inject
    public ConnectionPoolCreator(ConnectionFactoryCreator connectionFactoryCreator, R2DBCConfig r2DBCConfig) {
        this.connectionFactoryCreator = connectionFactoryCreator;
        this.connectionPool = createConnectionPool(r2DBCConfig);
    }

    private ConnectionPool createConnectionPool(R2DBCConfig r2DBCConfig) {

        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration
                .builder(connectionFactoryCreator.createFactory())
                .maxIdleTime(Duration.ofMillis(r2DBCConfig.getPoolMaxIdleTime()))
                .maxSize(r2DBCConfig.getPoolMaxSize())
                .build();

        return new ConnectionPool(poolConfiguration);
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }
}
