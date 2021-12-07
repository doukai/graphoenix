package io.graphoenix.r2dbc.connector.handler.operation;

import dagger.Component;
import io.graphoenix.r2dbc.connector.module.R2dbcConnectorModule;
import io.graphoenix.spi.handler.IOperationHandlerFactory;

import javax.inject.Singleton;

@Singleton
@Component(modules = R2dbcConnectorModule.class)
public interface OperationSQLExecuteHandlerFactory extends IOperationHandlerFactory {

    @Override
    OperationSQLExecuteHandler createHandler();
}
