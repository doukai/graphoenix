package io.graphoenix.core.handler;

import io.graphoenix.core.operation.Operation;
import reactor.core.publisher.Mono;

public interface OperationInterceptor {
    Mono<Operation> invoke(Operation operation);
}
