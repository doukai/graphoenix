package io.graphoenix.core.handler;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.dto.inputObjectType.MetaExpression;
import jakarta.enterprise.context.ApplicationScoped;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@ApplicationScoped
public class MetaExpressionHandler {
    private static final Collection<MetaExpressionInvoker> expressionInvokers = BeanContext.getMap(MetaExpressionInvoker.class).values();

    public <T extends MetaExpression> Mono<T> handle(String typeName, MetaExpression metaExpression, Class<T> tClass) {
        return Flux.fromIterable(expressionInvokers)
                .reduce(Mono.justOrEmpty(metaExpression), (metaExpressionMono, metaExpressionInvoker) -> metaExpressionMono.flatMap(result -> metaExpressionInvoker.invoke(typeName, result)).switchIfEmpty(metaExpressionInvoker.invoke(typeName, null)))
                .flatMap(metaExpressionMono -> metaExpressionMono)
                .map(tClass::cast);
    }
}
