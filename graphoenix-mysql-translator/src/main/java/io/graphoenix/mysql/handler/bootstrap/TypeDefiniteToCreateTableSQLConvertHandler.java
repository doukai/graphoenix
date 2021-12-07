package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import javax.inject.Inject;
import java.util.stream.Stream;

public class TypeDefiniteToCreateTableSQLConvertHandler implements IBootstrapHandler {

    private final GraphQLTypeToTable graphqlTypeToTable;

    @Inject
    public TypeDefiniteToCreateTableSQLConvertHandler(GraphQLTypeToTable graphqlTypeToTable) {
        this.graphqlTypeToTable = graphqlTypeToTable;
    }

    @Override
    public boolean execute(IPipelineContext context) {
        Stream<String> sqlStream = graphqlTypeToTable.createTablesSQL();
        context.add(sqlStream);
        return false;
    }
}
