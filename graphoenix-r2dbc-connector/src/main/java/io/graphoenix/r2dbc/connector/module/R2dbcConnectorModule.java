package io.graphoenix.r2dbc.connector.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.r2dbc.connector.handler.bootstrap.CreateTableSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.bootstrap.IntrospectionMutationExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.config.R2DBCConfig;

import javax.inject.Singleton;

import java.io.FileNotFoundException;
import java.io.IOException;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

@Module
public class R2dbcConnectorModule {

    @Provides
    @Singleton
    public R2DBCConfig config() {
        return YAML_CONFIG_UTIL.loadAs(R2DBCConfig.class);
    }

    @Provides
    @Singleton
    public IConnectionCreator connectionCreator() {
        return new ConnectionCreator(config());
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
