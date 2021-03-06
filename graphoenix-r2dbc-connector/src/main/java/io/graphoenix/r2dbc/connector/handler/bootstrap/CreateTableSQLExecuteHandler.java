package io.graphoenix.r2dbc.connector.handler.bootstrap;

import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

import java.util.stream.Stream;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

public class CreateTableSQLExecuteHandler implements IBootstrapHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Void transform(IGraphQLDocumentManager manager, Object sqlStream) {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, ConnectionConfiguration.class));
        TableCreator tableCreator = new TableCreator(connectionCreator);
        tableCreator.createTables((Stream<String>) sqlStream).block();
        return null;
    }
}
