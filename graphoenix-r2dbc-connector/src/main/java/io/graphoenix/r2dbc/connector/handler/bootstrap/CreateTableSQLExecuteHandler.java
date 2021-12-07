package io.graphoenix.r2dbc.connector.handler.bootstrap;

import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import javax.inject.Inject;
import java.util.stream.Stream;

public class CreateTableSQLExecuteHandler implements IBootstrapHandler {

    private final TableCreator tableCreator;

    @Inject
    public CreateTableSQLExecuteHandler(TableCreator tableCreator) {
        this.tableCreator = tableCreator;
    }

    @Override
    public boolean execute(IPipelineContext context) {
        Stream<String> sqlStream = context.pollStream(String.class);
        tableCreator.createTables(sqlStream).block();
        return false;
    }
}
