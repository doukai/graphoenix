package io.graphoenix.mysql.task;

import com.google.auto.service.AutoService;
import io.graphoenix.mysql.translator.GraphqlTypeToTable;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLTypeDefineToCreateSQLTask;

@AutoService(IGraphQLTypeDefineToCreateSQLTask.class)
public class GraphQLTypeDefineToCreateSQLTask implements IGraphQLTypeDefineToCreateSQLTask {

    private IGraphqlDocumentManager graphqlDocumentManager;

    private GraphQLTaskType type;

    @Override
    public GraphQLTaskType getType() {
        return this.type;
    }

    @Override
    public void init(Void input) {

    }

    @Override
    public void init(Void input, GraphQLTaskType type) {
        this.type = type;
    }

    @Override
    public void init(GraphQLTaskType type) {
        this.type = type;
    }

    @Override
    public void assign(IGraphqlDocumentManager manager) {
        this.graphqlDocumentManager = manager;
    }

    @Override
    public SQLStatements process() {
        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(this.graphqlDocumentManager);
        return new SQLStatements(graphqlTypeToTable.createTablesSql());
    }
}
