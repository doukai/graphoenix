package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

import java.util.stream.Stream;

public class TypeDefiniteToCreateTableSQLConvertHandler implements IBootstrapHandler {

    @Override
    public Stream<String> transform(IGraphQLDocumentManager manager, Object object) {
        GraphQLTypeToTable graphqlTypeToTable = new GraphQLTypeToTable(manager);
        return graphqlTypeToTable.createTablesSQL();
    }
}
