package io.graphoenix.mysql.handler.operation;

import io.graphoenix.common.manager.GraphQLFieldMapManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.mysql.translator.GraphQLArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.spi.dto.SelectionResult;
import io.graphoenix.spi.handler.IOperationHandler;

import java.util.stream.Stream;

public class OperationToSQLConvertHandler implements IOperationHandler {

    private GraphQLQueryToSelect graphqlQueryToSelect;
    private GraphQLMutationToStatements graphqlMutationToStatements;
    private IGraphQLDocumentManager manager;
//    private MysqlTranslateConfig config;

    @Override
    public void setupManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
        GraphQLFieldMapManager mapper = new GraphQLFieldMapManager(manager);
        this.graphqlQueryToSelect = new GraphQLQueryToSelect(manager, mapper, new GraphQLArgumentsToWhere(manager, mapper));
        this.graphqlMutationToStatements = new GraphQLMutationToStatements(manager, mapper, this.graphqlQueryToSelect);
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
