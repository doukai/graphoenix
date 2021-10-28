package io.graphoenix.graphql.builder.task;

import com.google.auto.service.AutoService;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLTypeRegisterTask;

@AutoService(IGraphQLTypeRegisterTask.class)
public class GraphQLTypeRegisterTask implements IGraphQLTypeRegisterTask {

    private IGraphqlDocumentManager graphqlDocumentManager;

    private String graphQL;

    private GraphQLTaskType type;

    @Override
    public GraphQLTaskType getType() {
        return this.type;
    }

    @Override
    public void init(String graphQL) {
        this.graphQL = graphQL;
    }

    @Override
    public void init(String graphQL, GraphQLTaskType type) {
        this.graphQL = graphQL;
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
    public Void process() {
        this.graphqlDocumentManager.registerDocument(this.graphQL);
        return null;
    }
}
