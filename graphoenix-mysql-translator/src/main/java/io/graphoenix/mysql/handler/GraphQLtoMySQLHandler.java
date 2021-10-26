package io.graphoenix.mysql.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.mysql.translator.GraphqlTypeToTable;

import io.graphoenix.meta.spi.IGraphQLToSQLHandler;

import java.util.List;

@AutoService(IGraphQLToSQLHandler.class)
public class GraphQLtoMySQLHandler implements IGraphQLToSQLHandler {

    private final GraphqlQueryToSelect graphqlQueryToSelect;
    private final GraphqlMutationToStatements graphqlMutationToStatements;
    private final GraphqlTypeToTable graphqlTypeToTable;

    public GraphQLtoMySQLHandler(GraphqlAntlrManager graphqlAntlrManager) {
        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        this.graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        this.graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, this.graphqlQueryToSelect);
        this.graphqlTypeToTable = new GraphqlTypeToTable(graphqlAntlrManager);
    }

    @Override
    public String queryOperationToSelectSQL(String queryOperationGraphQL) {
        return this.graphqlQueryToSelect.createSelectsSql(queryOperationGraphQL).get(0);
    }

    @Override
    public List<String> queryOperationToSelectSQLList(String queryOperationGraphQL) {
        return this.graphqlQueryToSelect.createSelectsSql(queryOperationGraphQL);
    }

    @Override
    public List<String> mutationOperationToMergeSQLList(String mutationOperationGraphQL) {
        return this.graphqlMutationToStatements.createStatementsSql(mutationOperationGraphQL);
    }

    @Override
    public List<String> typeDefinitionToCreateTableSql(String typeDefinitionGraphQL) {
        return this.graphqlTypeToTable.createTablesSql(typeDefinitionGraphQL);
    }

    @Override
    public List<String> typeDefinitionToAlterTableSql(String typeDefinitionGraphQL) {
        return null;
    }
}
