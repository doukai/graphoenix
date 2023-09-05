package io.graphoenix.core.handler;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.operation.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@ApplicationScoped
public class OperationInterceptorHandler {

    public static Collection<Provider<OperationInterceptor>> interceptorProviders = BeanContext.getProviderMap(OperationInterceptor.class).values();

    public Mono<Operation> handle(Operation operation) {
        return Flux.fromIterable(interceptorProviders)
                .reduce(Mono.just(operation), ((operationMono, operationInterceptorProvider) -> operationMono.flatMap(previous -> operationInterceptorProvider.get().invoke(previous))))
                .flatMap(operationMono -> operationMono)
                .switchIfEmpty(Mono.just(operation));
    }
}
