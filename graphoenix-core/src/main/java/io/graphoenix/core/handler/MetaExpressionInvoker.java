package io.graphoenix.core.handler;

import io.graphoenix.core.dto.inputObjectType.MetaExpression;
import reactor.core.publisher.Mono;

public interface MetaExpressionInvoker {

    Mono<MetaExpression> invoke(String typeName, MetaExpression metaExpression);
}
