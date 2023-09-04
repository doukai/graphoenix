package io.graphoenix.core.handler;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.dto.inputObjectType.MetaInput;
import jakarta.enterprise.context.ApplicationScoped;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@ApplicationScoped
public class MetaInputHandler {
    private static final Collection<MetaInputInvoker> inputInvokers = BeanContext.getMap(MetaInputInvoker.class).values();

    public <T extends MetaInput> Mono<T> handle(String typeName, MetaInput metaInput, Class<T> tClass) {
        return Flux.fromIterable(inputInvokers)
                .reduce(Mono.justOrEmpty(metaInput), (metaInputMono, metaInputInvoker) -> metaInputMono.flatMap(result -> metaInputInvoker.invoke(typeName, result)).switchIfEmpty(metaInputInvoker.invoke(typeName, null)))
                .flatMap(metaInputMono -> metaInputMono)
                .map(tClass::cast);
    }
}
