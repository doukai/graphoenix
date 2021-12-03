package io.graphoenix.r2dbc.connector.handler.bootstrap;

import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.spi.config.R2DBCConfig;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import java.util.stream.Stream;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

public class CreateTableSQLExecuteHandler implements IBootstrapHandler {

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(IPipelineContext context) {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, R2DBCConfig.class));
        TableCreator tableCreator = new TableCreator(connectionCreator);
        Stream<String> sqlStream = context.poll(Stream.class);
        tableCreator.createTables(sqlStream).block();
        return true;
    }
}
