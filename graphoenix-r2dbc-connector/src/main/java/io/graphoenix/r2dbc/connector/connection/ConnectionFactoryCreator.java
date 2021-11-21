package io.graphoenix.r2dbc.connector.connection;

import io.graphoenix.spi.config.R2DBCConfig;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

public enum ConnectionFactoryCreator {

    FACTORY_CREATOR;

    public ConnectionFactory createFactory(R2DBCConfig r2DBCConfig) {

        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, r2DBCConfig.getDriver())
                .option(ConnectionFactoryOptions.PROTOCOL, r2DBCConfig.getProtocol())
                .option(ConnectionFactoryOptions.HOST, r2DBCConfig.getHost())
                .option(ConnectionFactoryOptions.PORT, r2DBCConfig.getPort())
                .option(ConnectionFactoryOptions.USER, r2DBCConfig.getUser())
                .option(ConnectionFactoryOptions.PASSWORD, r2DBCConfig.getPassword())
                .option(ConnectionFactoryOptions.DATABASE, r2DBCConfig.getDatabase())
                .build();

        return ConnectionFactories.get(options);
    }
}
