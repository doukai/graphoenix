package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

public interface IPipelineContext {

    IPipelineContext getInstance();

    IGraphQLDocumentManager getManager();

    void setManager(IGraphQLDocumentManager manager);

    void addStatus(Enum<?> status);

    <T> T getStatus(Class<T> clazz) throws ClassCastException;

    IPipelineContext add(Object object);

    <T> T poll(Class<T> clazz);

    Object poll();

    <T> T element(Class<T> clazz) throws ClassCastException;

    Object element();
}
