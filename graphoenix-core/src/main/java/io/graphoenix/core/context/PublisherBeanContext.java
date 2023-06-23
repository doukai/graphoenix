package io.graphoenix.core.context;

import io.graphoenix.spi.context.ScopeInstances;
import jakarta.inject.Named;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;

public class PublisherBeanContext {

    private static final String SCOPE_INSTANCES_KEY = "scopeInstances";

    public static <T> Mono<T> getMono(Class<T> beanClass) {
        return getMono(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> getMono(Class<T> beanClass, String name) {
        return Mono.deferContextual(contextView ->
                Mono.justOrEmpty(
                        contextView.getOrEmpty(SCOPE_INSTANCES_KEY)
                                .map(scopeInstances -> (ScopeInstances) scopeInstances)
                                .flatMap(scopeInstances -> Optional.ofNullable(scopeInstances.get(beanClass)))
                                .flatMap(map -> Optional.ofNullable(map.get(name)))
                                .map(bean -> (T) bean)
                )
        );
    }

    public static <T> Flux<T> getFlux(Class<T> beanClass) {
        return getFlux(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Flux<T> getFlux(Class<T> beanClass, String name) {
        return Flux.deferContextual(contextView ->
                Mono.justOrEmpty(
                        contextView.getOrEmpty(SCOPE_INSTANCES_KEY)
                                .map(scopeInstances -> (ScopeInstances) scopeInstances)
                                .flatMap(scopeInstances -> Optional.ofNullable(scopeInstances.get(beanClass)))
                                .flatMap(map -> Optional.ofNullable(map.get(name)))
                                .map(bean -> (T) bean)
                )
        );
    }

    public static Context of(Object... beans) {
        ScopeInstances scopeInstances = new ScopeInstances();
        for (Object bean : beans) {
            scopeInstances.get(bean.getClass()).putIfAbsent(bean.getClass().getName(), bean);
            if (bean.getClass().isAnnotationPresent(Named.class)) {
                scopeInstances.get(bean.getClass()).putIfAbsent(bean.getClass().getAnnotation(Named.class).value(), bean);
            }
        }
        return Context.of(SCOPE_INSTANCES_KEY, scopeInstances);
    }
}
