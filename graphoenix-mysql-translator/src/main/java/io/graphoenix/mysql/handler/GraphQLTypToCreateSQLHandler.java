package io.graphoenix.mysql.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.mysql.translator.GraphqlTypeToTable;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.IGraphQLTypeToCrateSQLHandler;

@AutoService(IGraphQLTypeToCrateSQLHandler.class)
public class GraphQLTypToCreateSQLHandler implements IGraphQLTypeToCrateSQLHandler {

    @Override
    public SQLStatements convert(IGraphqlDocumentManager manager) {
        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(manager);
        return new SQLStatements(graphqlTypeToTable.createTablesSql());
    }
}
