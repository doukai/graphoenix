package io.graphoenix.r2dbc.connector.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.r2dbc.connector.connection.ConnectionFactoryCreator;
import io.graphoenix.r2dbc.connector.connection.ConnectionPoolCreator;
import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.r2dbc.connector.handler.bootstrap.CreateTableSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.bootstrap.IntrospectionMutationExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.config.R2DBCConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Module
public class R2dbcConnectorModule {

    @ConfigProperty
    public R2DBCConfig r2dbcConfig;

    @Provides
    @Singleton
    public ConnectionFactoryCreator connectionFactoryCreator() {
        return new ConnectionFactoryCreator(r2dbcConfig);
    }

    @Provides
    @Singleton
    public ConnectionPoolCreator connectionPoolCreator() {
        return new ConnectionPoolCreator(connectionFactoryCreator(), r2dbcConfig);
    }

    @Provides
    @Singleton
    public IConnectionCreator connectionCreator() {
        return new ConnectionCreator(connectionFactoryCreator(), connectionPoolCreator(), r2dbcConfig);
    }

    @Provides
    @Singleton
    public R2dbcParameterProcessor r2dbcParameterProcessor() {
        return new R2dbcParameterProcessor();
    }

    @Provides
    @Singleton
    public MutationExecutor mutationExecutor() {
        return new MutationExecutor(connectionCreator());
    }

    @Provides
    @Singleton
    public QueryExecutor queryExecutor() {
        return new QueryExecutor(connectionCreator());
    }

    @Provides
    @Singleton
    public TableCreator tableCreator() {
        return new TableCreator(connectionCreator());
    }

    @Provides
    @Singleton
    public OperationSQLExecuteHandler operationSQLExecuteHandler() {
        return new OperationSQLExecuteHandler(queryExecutor(), mutationExecutor(), r2dbcParameterProcessor());
    }

    @Provides
    @Singleton
    public IntrospectionMutationExecuteHandler introspectionMutationExecuteHandler() {
        return new IntrospectionMutationExecuteHandler(mutationExecutor());
    }

    @Provides
    @Singleton
    public CreateTableSQLExecuteHandler createTableSQLExecuteHandler() {
        return new CreateTableSQLExecuteHandler(tableCreator());
    }
}
