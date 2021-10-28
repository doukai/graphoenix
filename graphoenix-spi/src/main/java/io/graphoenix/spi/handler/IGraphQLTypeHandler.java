package io.graphoenix.spi.handler;

public interface IGraphQLTypeHandler<I, O> {

    O convert(I input);
}
