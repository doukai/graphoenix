package io.graphoenix.spi.handler;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Initialized;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class ScopeEventResolver {

    private static volatile ServiceLoader<ScopeEvent> scopeEvents = null;

    public static ServiceLoader<ScopeEvent> getScopeEvents() {
        if (scopeEvents == null) {
            synchronized (ScopeEventResolver.class) {
                if (scopeEvents != null) {
                    return scopeEvents;
                }
                ClassLoader cl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
                if (cl == null) {
                    cl = ScopeEventResolver.class.getClassLoader();
                }
                scopeEvents = ServiceLoader.load(ScopeEvent.class, cl);
            }
        }
        return scopeEvents;
    }

    public static Mono<Void> initialized(Class<? extends Annotation> scope) {
        return initialized(new HashMap<>(), scope);
    }

    public static Mono<Void> initialized(Map<String, Object> context, Class<? extends Annotation> scope) {
        return Flux.fromIterable(
                        getScopeEvents().stream()
                                .filter(scopeEventProvider -> scopeEventProvider.type().isAnnotationPresent(Initialized.class))
                                .filter(scopeEventProvider -> scopeEventProvider.type().getAnnotation(Initialized.class).value().equals(scope))
                                .sorted(Comparator.comparing(scopeEventProvider -> getPriority(scopeEventProvider.type())))
                                .collect(Collectors.toList())
                )
                .flatMap(scopeEventProvider -> scopeEventProvider.get().fireAsync(context))
                .then();
    }

    private static int getPriority(Class<? extends ScopeEvent> type) {
        return Optional.ofNullable(type.getAnnotation(Priority.class)).map(Priority::value).orElse(Integer.MAX_VALUE);
    }
}
