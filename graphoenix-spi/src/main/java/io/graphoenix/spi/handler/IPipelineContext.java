package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

public interface IPipelineContext {

    IPipelineContext getInstance();

    IGraphQLDocumentManager getManager();

    void setManager(IGraphQLDocumentManager manager);

    IPipelineContext add(Object object);

    <T> T poll(Class<T> clazz);

    Object poll();
}
