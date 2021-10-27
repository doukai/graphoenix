package io.graphoenix.mysql.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.meta.antlr.IGraphqlDocumentManager;
import io.graphoenix.meta.dto.GraphQLRequestBody;
import io.graphoenix.meta.dto.SQLStatements;
import io.graphoenix.mysql.config.MysqlTranslateConfig;
import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.mysql.translator.GraphqlTypeToTable;

import io.graphoenix.meta.spi.IGraphQLToSQLHandler;

@AutoService(IGraphQLToSQLHandler.class)
public class GraphQLtoMySQLHandler implements IGraphQLToSQLHandler {

    private GraphqlQueryToSelect graphqlQueryToSelect;
    private GraphqlMutationToStatements graphqlMutationToStatements;
    private GraphqlTypeToTable graphqlTypeToTable;
    private MysqlTranslateConfig config;

    @Override
    public void assign(IGraphqlDocumentManager manager) {
        this.graphqlQueryToSelect = new GraphqlQueryToSelect(manager, new GraphqlArgumentsToWhere(manager));
        this.graphqlMutationToStatements = new GraphqlMutationToStatements(manager, this.graphqlQueryToSelect);
        this.graphqlTypeToTable = new GraphqlTypeToTable(manager);
        this.config = config;
    }

    @Override
    public SQLStatements query(GraphQLRequestBody requestBody) {
        return new SQLStatements(this.graphqlQueryToSelect.createSelectsSql(requestBody.getQuery()));
    }

    @Override
    public SQLStatements mutation(GraphQLRequestBody requestBody) {
        return new SQLStatements(this.graphqlMutationToStatements.createStatementsSql(requestBody.getQuery()));
    }

    @Override
    public SQLStatements subscription(GraphQLRequestBody requestBody) {
        return null;
    }
}
