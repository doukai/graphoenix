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

    public static Context of(Class<?> class1, Object bean1) {
        ScopeInstances scopeInstances = new ScopeInstances();
        scopeInstances.get(class1).putIfAbsent(class1.getName(), bean1);
        if (class1.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean1.getClass()).putIfAbsent(class1.getAnnotation(Named.class).value(), bean1);
        }
        return Context.of(SCOPE_INSTANCES_KEY, scopeInstances);
    }

    public static Context of(Class<?> class1, Object bean1, Class<?> class2, Object bean2) {
        ScopeInstances scopeInstances = new ScopeInstances();
        scopeInstances.get(class1).putIfAbsent(class1.getName(), bean1);
        if (class1.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean1.getClass()).putIfAbsent(class1.getAnnotation(Named.class).value(), bean1);
        }
        scopeInstances.get(class2).putIfAbsent(class2.getName(), bean2);
        if (class2.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean2.getClass()).putIfAbsent(class2.getAnnotation(Named.class).value(), bean2);
        }
        return Context.of(SCOPE_INSTANCES_KEY, scopeInstances);
    }

    public static Context of(Class<?> class1, Object bean1, Class<?> class2, Object bean2, Class<?> class3, Object bean3) {
        ScopeInstances scopeInstances = new ScopeInstances();
        scopeInstances.get(class1).putIfAbsent(class1.getName(), bean1);
        if (class1.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean1.getClass()).putIfAbsent(class1.getAnnotation(Named.class).value(), bean1);
        }
        scopeInstances.get(class2).putIfAbsent(class2.getName(), bean2);
        if (class2.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean2.getClass()).putIfAbsent(class2.getAnnotation(Named.class).value(), bean2);
        }
        scopeInstances.get(class3).putIfAbsent(class3.getName(), bean3);
        if (class3.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean3.getClass()).putIfAbsent(class3.getAnnotation(Named.class).value(), bean3);
        }
        return Context.of(SCOPE_INSTANCES_KEY, scopeInstances);
    }

    public static Context of(Class<?> class1, Object bean1, Class<?> class2, Object bean2, Class<?> class3, Object bean3, Class<?> class4, Object bean4) {
        ScopeInstances scopeInstances = new ScopeInstances();
        scopeInstances.get(class1).putIfAbsent(class1.getName(), bean1);
        if (class1.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean1.getClass()).putIfAbsent(class1.getAnnotation(Named.class).value(), bean1);
        }
        scopeInstances.get(class2).putIfAbsent(class2.getName(), bean2);
        if (class2.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean2.getClass()).putIfAbsent(class2.getAnnotation(Named.class).value(), bean2);
        }
        scopeInstances.get(class3).putIfAbsent(class3.getName(), bean3);
        if (class3.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean3.getClass()).putIfAbsent(class3.getAnnotation(Named.class).value(), bean3);
        }
        scopeInstances.get(class4).putIfAbsent(class4.getName(), bean4);
        if (class4.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean4.getClass()).putIfAbsent(class4.getAnnotation(Named.class).value(), bean4);
        }
        return Context.of(SCOPE_INSTANCES_KEY, scopeInstances);
    }

    public static Context of(Class<?> class1, Object bean1, Class<?> class2, Object bean2, Class<?> class3, Object bean3, Class<?> class4, Object bean4, Class<?> class5, Object bean5) {
        ScopeInstances scopeInstances = new ScopeInstances();
        scopeInstances.get(class1).putIfAbsent(class1.getName(), bean1);
        if (class1.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean1.getClass()).putIfAbsent(class1.getAnnotation(Named.class).value(), bean1);
        }
        scopeInstances.get(class2).putIfAbsent(class2.getName(), bean2);
        if (class2.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean2.getClass()).putIfAbsent(class2.getAnnotation(Named.class).value(), bean2);
        }
        scopeInstances.get(class3).putIfAbsent(class3.getName(), bean3);
        if (class3.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean3.getClass()).putIfAbsent(class3.getAnnotation(Named.class).value(), bean3);
        }
        scopeInstances.get(class4).putIfAbsent(class4.getName(), bean4);
        if (class4.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean4.getClass()).putIfAbsent(class4.getAnnotation(Named.class).value(), bean4);
        }
        scopeInstances.get(class5).putIfAbsent(class5.getName(), bean5);
        if (class5.isAnnotationPresent(Named.class)) {
            scopeInstances.get(bean5.getClass()).putIfAbsent(class5.getAnnotation(Named.class).value(), bean5);
        }
        return Context.of(SCOPE_INSTANCES_KEY, scopeInstances);
    }
}
