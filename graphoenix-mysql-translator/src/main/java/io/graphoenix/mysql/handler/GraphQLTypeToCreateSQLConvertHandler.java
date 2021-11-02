package io.graphoenix.mysql.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.mysql.translator.GraphqlTypeToTable;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.bootstrap.IGraphQLTypeToCreateSQLConvertHandler;

@AutoService(IGraphQLTypeToCreateSQLConvertHandler.class)
public class GraphQLTypeToCreateSQLConvertHandler implements IGraphQLTypeToCreateSQLConvertHandler {

    @Override
    public SQLStatements transform(IGraphqlDocumentManager manager, Void object) {
        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(manager);
        return new SQLStatements(graphqlTypeToTable.createTablesSql());
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {
    }
}
