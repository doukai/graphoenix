package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.task.IGraphQLTask;

public interface IGraphQLOperationPipeline<I, O> {

    @SuppressWarnings("rawtypes")
    <H extends IGraphQLOperationHandler> IGraphQLOperationPipeline<I, O> push(Class<H> handlerClass);

    @SuppressWarnings("rawtypes")
    <H extends IGraphQLTask> IGraphQLOperationPipeline<I, O> task(Class<H> handlerClass, Object input);

    @SuppressWarnings("rawtypes")
    <H extends IGraphQLTask> IGraphQLOperationPipeline<I, O> task(Class<H> handlerClass);

    @SuppressWarnings({"rawtypes"})
    <H extends IGraphQLTask> IGraphQLOperationPipeline<I, O> asyncTask(Class<H> handlerClass, Object input);

    @SuppressWarnings("rawtypes")
    <H extends IGraphQLTask> IGraphQLOperationPipeline<I, O> asyncTask(Class<H> handlerClass);

    IGraphQLOperationPipeline<I, O> build();

    IGraphqlDocumentManager getManager();

    OperationType getOperationType(I request);

    O order(I request);

    IGraphQLOperationPipeline<I, O> runTask();
}
