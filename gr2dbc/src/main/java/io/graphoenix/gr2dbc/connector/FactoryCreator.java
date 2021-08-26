package io.graphoenix.gr2dbc.connector;

import io.graphoenix.gr2dbc.config.ConnectionConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;

public enum FactoryCreator {

    FACTORY_CREATOR;

    public ConnectionFactory createFactory(ConnectionConfiguration connectionConfiguration) {
        MariadbConnectionConfiguration mariadbConnectionConfiguration = MariadbConnectionConfiguration.builder()
                .host(connectionConfiguration.getHost())
                .port(connectionConfiguration.getPort())
                .username(connectionConfiguration.getUsername())
                .password(connectionConfiguration.getPassword())
                .database(connectionConfiguration.getDatabase())
                .build();

        return new MariadbConnectionFactory(mariadbConnectionConfiguration);
    }
}
