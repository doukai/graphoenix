package io.graphoenix.r2dbc.connector.handler.bootstrap;

import dagger.Component;
import io.graphoenix.r2dbc.connector.module.R2dbcConnectorModule;
import io.graphoenix.spi.handler.IBootstrapHandlerFactory;

import javax.inject.Singleton;

@Singleton
@Component(modules = R2dbcConnectorModule.class)
public interface CreateTableSQLExecuteHandlerFactory extends IBootstrapHandlerFactory {

    @Override
    CreateTableSQLExecuteHandler createHandler();
}
