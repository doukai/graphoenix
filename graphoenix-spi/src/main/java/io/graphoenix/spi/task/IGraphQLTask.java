package io.graphoenix.spi.task;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

import java.io.IOException;

public interface IGraphQLTask<I, O> {

    GraphQLTaskType getType();

    void init(I input);

    void init(I input, GraphQLTaskType type);

    void init(GraphQLTaskType type);

    void assign(IGraphqlDocumentManager manager);

    O process() throws IOException;
}
