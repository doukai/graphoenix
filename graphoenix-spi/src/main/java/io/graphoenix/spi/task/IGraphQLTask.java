package io.graphoenix.spi.task;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.IGraphQLTypeHandler;

import java.io.IOException;

public interface IGraphQLTask<I> {

    GraphQLTaskType getType();

    void init(I input, GraphQLTaskType type);

    void init(GraphQLTaskType type);

    void assign(IGraphqlDocumentManager manager);

    @SuppressWarnings("rawtypes")
    <H extends IGraphQLTypeHandler> IGraphQLTask<I> push(Class<H> handlerClass);

    void process() throws IOException;
}
