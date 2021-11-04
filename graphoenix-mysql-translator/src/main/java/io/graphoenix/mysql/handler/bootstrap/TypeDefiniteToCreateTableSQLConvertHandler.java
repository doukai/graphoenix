package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphqlTypeToTable;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.IBootstrapHandler;

public class TypeDefiniteToCreateTableSQLConvertHandler implements IBootstrapHandler {

    @Override
    public SQLStatements transform(IGraphqlDocumentManager manager, Object object) {
        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(manager);
        return new SQLStatements(graphqlTypeToTable.createTablesSql());
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {
    }
}
