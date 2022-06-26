package io.graphoenix.core.context;

import io.graphoenix.spi.context.RequestInstances;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestScope {
    public static final String REQUEST_ID = "requestId";

    private static final Map<String, RequestInstances> REQUEST_SCOPE_CACHE = new ConcurrentHashMap<>();

    private RequestScope() {
    }

    public static <T> Mono<T> get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> get(Class<T> beanClass, String name) {
        return Mono.deferContextual(contextView -> Mono.just((String) contextView.get(REQUEST_ID)))
                .flatMap(requestId -> {
                            if (!REQUEST_SCOPE_CACHE.containsKey(requestId)) {
                                return Mono.empty();
                            }
                            if (!REQUEST_SCOPE_CACHE.get(requestId).get(beanClass).containsKey(name)) {
                                return Mono.empty();
                            }
                            return Mono.just((T) REQUEST_SCOPE_CACHE.get(requestId).get(beanClass).get(name));
                        }
                );
    }

    public static <T> Mono<T> cacheAndGet(Class<T> beanClass, T instance) {
        return cacheAndGet(beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> cacheAndGet(Class<T> beanClass, String name, T instance) {
        return Mono.deferContextual(contextView -> Mono.just((String) contextView.get(REQUEST_ID)))
                .map(requestId -> {
                            if (!REQUEST_SCOPE_CACHE.containsKey(requestId)) {
                                REQUEST_SCOPE_CACHE.putIfAbsent(requestId, new RequestInstances());
                            }
                            if (!REQUEST_SCOPE_CACHE.get(requestId).get(beanClass).containsKey(name)) {
                                REQUEST_SCOPE_CACHE.get(requestId).get(beanClass).putIfAbsent(name, instance);
                            }
                            return (T) REQUEST_SCOPE_CACHE.get(requestId).get(beanClass).get(name);
                        }
                );
    }

    public static <T> void put(String requestId, Class<T> beanClass, T instance) {
        put(requestId, beanClass, beanClass.getName(), instance);
    }

    public static <T> void put(String requestId, Class<T> beanClass, String name, T instance) {
        if (!REQUEST_SCOPE_CACHE.containsKey(requestId)) {
            REQUEST_SCOPE_CACHE.putIfAbsent(requestId, new RequestInstances());
        }
        if (!REQUEST_SCOPE_CACHE.get(requestId).get(beanClass).containsKey(name)) {
            REQUEST_SCOPE_CACHE.get(requestId).get(beanClass).putIfAbsent(name, instance);
        }
    }

    public static <T> void remove(String requestId) {
        REQUEST_SCOPE_CACHE.remove(requestId);
    }
}
