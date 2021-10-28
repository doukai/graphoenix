package io.graphoenix.r2dbc.connector.task;

import com.google.auto.service.AutoService;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLTypeDefineTask;

import java.io.IOException;

@AutoService(IGraphQLTypeDefineTask.class)
public class GraphQLTypeDefineTask implements IGraphQLTypeDefineTask {

    private IGraphqlDocumentManager graphqlDocumentManager;

    private GraphQLTaskType type;

    @Override
    public GraphQLTaskType getType() {
        return this.type;
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
    public void process() throws IOException {

        SQLStatements sqlStatements = this.graphQLTypToCreateSQLHandler.translate(this.graphqlDocumentManager);


    }
}
