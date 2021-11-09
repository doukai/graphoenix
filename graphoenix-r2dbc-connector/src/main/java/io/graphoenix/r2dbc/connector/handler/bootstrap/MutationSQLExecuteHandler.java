package io.graphoenix.r2dbc.connector.handler.bootstrap;

import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

import java.util.stream.Stream;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

public class MutationSQLExecuteHandler implements IBootstrapHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Void transform(IGraphqlDocumentManager manager, Object sqlStream) {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, ConnectionConfiguration.class));
        MutationExecutor mutationExecutor = new MutationExecutor(connectionCreator);
        mutationExecutor.executeMutations((Stream<String>) sqlStream);
        return null;
    }
}
