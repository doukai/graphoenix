package io.graphoenix.r2dbc.connector.task;

import com.google.auto.service.AutoService;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.r2dbc.connector.TableCreator;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.ICreateSQLTask;

import static io.graphoenix.common.config.YamlConfigLoader.YAML_CONFIG_LOADER;

@AutoService(ICreateSQLTask.class)
public class CreateSQLTask implements ICreateSQLTask {

    private SQLStatements sqlStatements;

    private GraphQLTaskType type;

    private TableCreator tableCreator;

    @Override
    public GraphQLTaskType getType() {
        return this.type;
    }

    @Override
    public void init(SQLStatements sqlStatements) {
        this.sqlStatements = sqlStatements;
    }

    @Override
    public void init(SQLStatements sqlStatements, GraphQLTaskType type) {
        this.type = type;
        this.sqlStatements = sqlStatements;
    }

    @Override
    public void init(GraphQLTaskType type) {
        this.type = type;
    }

    @Override
    public void assign(IGraphqlDocumentManager manager) {
    }

    @Override
    public Void process() {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_LOADER.loadAs(Hammurabi.CONFIG_FILE_NAME, ConnectionConfiguration.class));
        this.tableCreator = new TableCreator(connectionCreator);
        this.tableCreator.createTables(sqlStatements.getSqlStatements()).block();
        return null;
    }
}
