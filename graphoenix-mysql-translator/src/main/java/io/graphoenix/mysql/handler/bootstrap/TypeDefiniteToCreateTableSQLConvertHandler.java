package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import java.util.stream.Stream;

public class TypeDefiniteToCreateTableSQLConvertHandler implements IBootstrapHandler {

    @Override
    public boolean execute(IPipelineContext context) {
        IGraphQLDocumentManager manager = context.getManager();
        GraphQLTypeToTable graphqlTypeToTable = new GraphQLTypeToTable(manager);
        Stream<String> sqlStream = graphqlTypeToTable.createTablesSQL();
        context.add(sqlStream);
        return true;
    }
}
