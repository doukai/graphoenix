package io.graphoenix.r2dbc.connector.handler;

import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.bootstrap.sql.IMutationSQLExecuteHandler;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

public class MutationSQLExecuteHandler implements IMutationSQLExecuteHandler {
    @Override
    public Void transform(IGraphqlDocumentManager manager, SQLStatements sqlStatements) {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, ConnectionConfiguration.class));
        MutationExecutor mutationExecutor = new MutationExecutor(connectionCreator);
        mutationExecutor.executeMutations(sqlStatements.getSqlStatements());
        return null;
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {

    }
}
