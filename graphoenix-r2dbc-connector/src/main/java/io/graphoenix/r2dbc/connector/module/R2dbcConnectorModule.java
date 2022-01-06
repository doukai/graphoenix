package io.graphoenix.r2dbc.connector.module;

import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.r2dbc.connector.connection.ConnectionFactoryCreator;
import io.graphoenix.r2dbc.connector.connection.ConnectionPoolCreator;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.r2dbc.connector.handler.OperationSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.r2dbc.connector.config.R2DBCConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Module
public class R2dbcConnectorModule {

    @ConfigProperty
    public R2DBCConfig r2DBCConfig;

    @Provides
    @Singleton
    public ConnectionFactoryCreator connectionFactoryCreator() {
        return new ConnectionFactoryCreator(r2DBCConfig);
    }

    @Provides
    @Singleton
    public ConnectionPoolCreator connectionPoolCreator() {
        return new ConnectionPoolCreator(connectionFactoryCreator(), r2DBCConfig);
    }

    @Provides
    @Singleton
    public ConnectionCreator connectionCreator() {
        return new ConnectionCreator(connectionFactoryCreator(), connectionPoolCreator(), r2DBCConfig);
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
}
