package io.graphoenix.mysql.handler.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.spi.dto.SelectionResult;
import io.graphoenix.spi.handler.IOperationHandler;

import java.util.stream.Stream;

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
    public String query(Object graphQL) throws Exception {
        manager.registerFragment((String) graphQL);
        return this.graphqlQueryToSelect.createSelectSQL((String) graphQL);
    }

    @Override
    public String queryAsync(Object graphQL) throws Exception {
        return query(graphQL);
    }

    @Override
    public Stream<SelectionResult<String>> querySelectionsAsync(Object graphQL) throws Exception {
        manager.registerFragment((String) graphQL);
        return this.graphqlQueryToSelect.createSelectsSQL((String) graphQL);
    }

    @Override
    public Stream<String> mutation(Object graphQL) throws Exception {
        manager.registerFragment((String) graphQL);
        return this.graphqlMutationToStatements.createStatementsSQL((String) graphQL);
    }

    @Override
    public Stream<String> mutationAsync(Object graphQL) throws Exception {
        return mutation(graphQL);
    }

    @Override
    public String subscription(Object graphQL) throws Exception {
        return query(graphQL);
    }
}
