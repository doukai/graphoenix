package io.graphoenix.r2dbc.connector;

import io.graphoenix.r2dbc.config.ConnectionConfiguration;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;

import java.time.Duration;

public enum ConnectionPoolCreator {

    CONNECTION_POOL_CREATOR;

    public ConnectionPool createConnectionPool(ConnectionConfiguration connectionConfiguration) {
        MariadbConnectionConfiguration mariadbConnectionConfiguration = MariadbConnectionConfiguration.builder()
                .host(connectionConfiguration.getHost())
                .port(connectionConfiguration.getPort())
                .username(connectionConfiguration.getUsername())
                .password(connectionConfiguration.getPassword())
                .database(connectionConfiguration.getDatabase())
                .build();

        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration
                .builder(new MariadbConnectionFactory(mariadbConnectionConfiguration))
                .maxIdleTime(Duration.ofMillis(connectionConfiguration.getMaxIdleTime()))
                .maxSize(connectionConfiguration.getMaxSize())
                .build();

        return new ConnectionPool(poolConfiguration);
    }
}
