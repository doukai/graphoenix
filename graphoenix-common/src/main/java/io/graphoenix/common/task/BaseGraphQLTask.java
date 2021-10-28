package io.graphoenix.common.task;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLTask;

public abstract class BaseGraphQLTask<I,O> implements IGraphQLTask<I,O> {

    private IGraphqlDocumentManager graphqlDocumentManager;

    private GraphQLTaskType type;

    @Override
    public GraphQLTaskType getType() {
        return null;
    }

    @Override
    public void init(I input, GraphQLTaskType type) {
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
}
