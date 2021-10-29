package io.graphoenix.mysql.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.GraphQLRequestBody;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.mysql.translator.GraphqlTypeToTable;

import io.graphoenix.spi.handler.IGraphQLToSQLHandler;

@AutoService(IGraphQLToSQLHandler.class)
public class GraphQLtoMySQLHandler implements IGraphQLToSQLHandler {

    private GraphqlQueryToSelect graphqlQueryToSelect;
    private GraphqlMutationToStatements graphqlMutationToStatements;
    private GraphqlTypeToTable graphqlTypeToTable;
    private IGraphqlDocumentManager manager;
//    private MysqlTranslateConfig config;

    @Override
    public void assign(IGraphqlDocumentManager manager) {
        this.manager = manager;
        this.graphqlQueryToSelect = new GraphqlQueryToSelect(manager, new GraphqlArgumentsToWhere(manager));
        this.graphqlMutationToStatements = new GraphqlMutationToStatements(manager, this.graphqlQueryToSelect);
        this.graphqlTypeToTable = new GraphqlTypeToTable(manager);
//        this.config = YAML_CONFIG_LOADER.loadAs(Hammurabi.configName, MysqlTranslateConfig.class);
    }

    @Override
    public SQLStatements query(GraphQLRequestBody requestBody) {
        this.manager.registerFragment(requestBody.getQuery());
        return new SQLStatements(this.graphqlQueryToSelect.createSelectsSql(requestBody.getQuery()));
    }

    @Override
    public SQLStatements mutation(GraphQLRequestBody requestBody) {
        this.manager.registerFragment(requestBody.getQuery());
        return new SQLStatements(this.graphqlMutationToStatements.createStatementsSql(requestBody.getQuery()));
    }

    @Override
    public SQLStatements subscription(GraphQLRequestBody requestBody) {
        this.manager.registerFragment(requestBody.getQuery());
        return null;
    }
}
