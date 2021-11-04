package io.graphoenix.mysql.handler.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;

import io.graphoenix.spi.handler.IOperationHandler;

public class OperationToSQLConvertHandler implements IOperationHandler {

    private GraphqlQueryToSelect graphqlQueryToSelect;
    private GraphqlMutationToStatements graphqlMutationToStatements;
    private IGraphqlDocumentManager manager;
//    private MysqlTranslateConfig config;

    @Override
    public void setupManager(IGraphqlDocumentManager manager) {
        this.manager = manager;
        this.graphqlQueryToSelect = new GraphqlQueryToSelect(manager, new GraphqlArgumentsToWhere(manager));
        this.graphqlMutationToStatements = new GraphqlMutationToStatements(manager, this.graphqlQueryToSelect);
//        this.config = YAML_CONFIG_LOADER.loadAs(Hammurabi.configName, MysqlTranslateConfig.class);
    }

    @Override
    public SQLStatements query(Object requestBody) {
        this.manager.registerFragment(((GraphQLRequest) requestBody).getQuery());
        return new SQLStatements(this.graphqlQueryToSelect.createSelectsSql(((GraphQLRequest) requestBody).getQuery()));
    }

    @Override
    public SQLStatements mutation(Object requestBody) {
        this.manager.registerFragment(((GraphQLRequest) requestBody).getQuery());
        return new SQLStatements(this.graphqlMutationToStatements.createStatementsSql(((GraphQLRequest) requestBody).getQuery()));
    }

    @Override
    public SQLStatements subscription(Object requestBody) {
        this.manager.registerFragment(((GraphQLRequest) requestBody).getQuery());
        return null;
    }
}
