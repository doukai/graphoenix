package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

public enum ConnectionFactoryCreator {

    FACTORY_CREATOR;

    public ConnectionFactory createFactory(ConnectionConfiguration connectionConfiguration) {

        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, connectionConfiguration.getDriver())
                .option(ConnectionFactoryOptions.PROTOCOL, connectionConfiguration.getProtocol())
                .option(ConnectionFactoryOptions.HOST, connectionConfiguration.getHost())
                .option(ConnectionFactoryOptions.PORT, connectionConfiguration.getPort())
                .option(ConnectionFactoryOptions.USER, connectionConfiguration.getUser())
                .option(ConnectionFactoryOptions.PASSWORD, connectionConfiguration.getPassword())
                .option(ConnectionFactoryOptions.DATABASE, connectionConfiguration.getDatabase())
                .build();

        return ConnectionFactories.get(options);
    }
}
