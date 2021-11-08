package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphqlTypeToTable;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

import java.util.stream.Stream;

public class TypeDefiniteToCreateTableSQLConvertHandler implements IBootstrapHandler {

    @Override
    public Stream<String> transform(IGraphqlDocumentManager manager, Object object) {
        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(manager);
        return graphqlTypeToTable.createTablesSQL();
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {
    }
}
