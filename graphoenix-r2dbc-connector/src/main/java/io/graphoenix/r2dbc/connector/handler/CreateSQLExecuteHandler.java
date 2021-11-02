package io.graphoenix.r2dbc.connector.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.r2dbc.connector.TableCreator;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.bootstrap.ICreateSQLExecuteHandler;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

@AutoService(ICreateSQLExecuteHandler.class)
public class CreateSQLExecuteHandler implements ICreateSQLExecuteHandler {

    @Override
    public Void transform(IGraphqlDocumentManager manager, SQLStatements sqlStatements) {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, ConnectionConfiguration.class));
        TableCreator tableCreator = new TableCreator(connectionCreator);
        tableCreator.createTables(sqlStatements.getSqlStatements()).block();
        return null;
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {
    }
}
