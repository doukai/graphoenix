package io.graphoenix.core.context;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import io.graphoenix.core.config.TimeoutConfig;
import io.graphoenix.spi.context.ScopeInstances;
import jakarta.enterprise.inject.Instance;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class RequestInstanceFactory {
    public static final String REQUEST_ID = "requestId";

    private static final AsyncLoadingCache<String, ScopeInstances> REQUEST_CACHE = buildCache();

    private static AsyncLoadingCache<String, ScopeInstances> buildCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .evictionListener((key, value, cause) -> Logger.info("request id: {} eviction", key))
                .removalListener((key, value, cause) -> Logger.info("request id: {} removed", key));
        Config config = ConfigFactory.load();
        if (config != null && config.hasPath("timeout")) {
            TimeoutConfig timeout = ConfigBeanFactory.create(config.getConfig("timeout"), TimeoutConfig.class);
            builder.expireAfterWrite(timeout.getRequest(), TimeUnit.SECONDS);
        } else {
            builder.expireAfterWrite(new TimeoutConfig().getRequest(), TimeUnit.SECONDS);
        }
        return builder.buildAsync(key -> new ScopeInstances());
    }

    private RequestInstanceFactory() {
    }

    public static <T> Mono<ScopeInstances> getScopeInstances() {
        return Mono.deferContextual(contextView -> Mono.fromFuture(REQUEST_CACHE.get(contextView.get(REQUEST_ID))));
    }

    public static <T> Mono<ScopeInstances> getScopeInstances(Class<T> beanClass, T instance) {
        return getScopeInstances(beanClass, beanClass.getName(), instance);
    }

    public static <T> Mono<ScopeInstances> getScopeInstances(Class<T> beanClass, String name, T instance) {
        return Mono.deferContextual(contextView -> Mono.fromFuture(REQUEST_CACHE.get(contextView.get(REQUEST_ID))))
                .map(scopeInstances -> {
                            scopeInstances.get(beanClass).putIfAbsent(name, instance);
                            return scopeInstances;
                        }
                );
    }

    public static <T> Mono<T> get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> get(Class<T> beanClass, String name) {
        return getScopeInstances().mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).get(name));
    }

    public static <T> Mono<T> get(Class<T> beanClass, T instance) {
        return get(beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> get(Class<T> beanClass, String name, T instance) {
        return getScopeInstances()
                .map(scopeInstances -> {
                            scopeInstances.get(beanClass).putIfAbsent(name, instance);
                            return scopeInstances;
                        }
                )
                .mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).get(name));
    }

    public static <T> Stream<T> getStream(Class<T> beanClass, String name) {
        return Flux.from(get(beanClass, name)).toStream();
    }

    public static <T> Stream<T> getStream(Class<T> beanClass, String name, T instance) {
        return Flux.from(get(beanClass, name, instance)).toStream();
    }

    public static <T> Instance<T> getInstance(Class<T> beanClass) {
        return getInstance(beanClass, beanClass.getName());
    }

    public static <T> Instance<T> getInstance(Class<T> beanClass, String name) {
        return new RequestInstance<>() {

            @Override
            public Instance<T> select(Annotation... qualifiers) {
                return getInstance(
                        beanClass,
                        getScopeInstances()
                                .flatMapMany(scopeInstances ->
                                        Flux.fromStream(scopeInstances.get(beanClass).entrySet().stream()
                                                .filter(entry ->
                                                        qualifiers.length == 0 ||
                                                                Arrays.stream(qualifiers)
                                                                        .anyMatch(qualifier -> qualifier.annotationType().getName().equals(entry.getKey()))
                                                )
                                        )
                                )
                                .toStream()
                );
            }

            @Override
            public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
                return getInstance(
                        subtype,
                        getScopeInstances()
                                .flatMapMany(scopeInstances ->
                                        Flux.fromStream(scopeInstances.get(beanClass).entrySet().stream()
                                                .filter(entry -> subtype.getSuperclass().equals(beanClass))
                                                .filter(entry ->
                                                        qualifiers.length == 0 ||
                                                                Arrays.stream(qualifiers)
                                                                        .anyMatch(qualifier -> qualifier.annotationType().getName().equals(entry.getKey()))
                                                )
                                        )
                                )
                                .toStream()
                );
            }

            @Override
            public Stream<T> stream() {
                return getStream(beanClass, name);
            }
        };
    }

    public static <T> Instance<T> getInstance(Class<T> beanClass, T instance) {
        return getInstance(beanClass, beanClass.getName(), instance);
    }

    public static <T> Instance<T> getInstance(Class<T> beanClass, String name, T instance) {
        return new RequestInstance<>() {

            @Override
            public Instance<T> select(Annotation... qualifiers) {
                return getInstance(
                        beanClass,
                        getScopeInstances(beanClass, name, instance)
                                .flatMapMany(scopeInstances ->
                                        Flux.fromStream(scopeInstances.get(beanClass).entrySet().stream()
                                                .filter(entry ->
                                                        qualifiers.length == 0 ||
                                                                Arrays.stream(qualifiers)
                                                                        .anyMatch(qualifier -> qualifier.annotationType().getName().equals(entry.getKey()))
                                                )
                                        )
                                )
                                .toStream()
                );
            }

            @Override
            public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
                return getInstance(
                        subtype,
                        getScopeInstances(beanClass, name, instance)
                                .flatMapMany(scopeInstances ->
                                        Flux.fromStream(scopeInstances.get(beanClass).entrySet().stream()
                                                .filter(entry -> subtype.getSuperclass().equals(beanClass))
                                                .filter(entry ->
                                                        qualifiers.length == 0 ||
                                                                Arrays.stream(qualifiers)
                                                                        .anyMatch(qualifier -> qualifier.annotationType().getName().equals(entry.getKey()))
                                                )
                                        )
                                )
                                .toStream()
                );
            }

            @Override
            public Stream<T> stream() {
                return getStream(beanClass, name, instance);
            }
        };
    }

    public static <T> Instance<T> getInstance(Class<T> beanClass, Stream<Map.Entry<String, Object>> instanceEntryStream) {
        return new RequestInstance<>() {

            @Override
            public Instance<T> select(Annotation... qualifiers) {
                return getInstance(
                        beanClass,
                        instanceEntryStream
                                .filter(entry ->
                                        qualifiers.length == 0 ||
                                                Arrays.stream(qualifiers)
                                                        .anyMatch(qualifier -> qualifier.annotationType().getName().equals(entry.getKey()))
                                )
                );
            }

            @Override
            public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
                return getInstance(
                        subtype,
                        instanceEntryStream
                                .filter(entry -> subtype.getSuperclass().equals(beanClass))
                                .filter(entry ->
                                        qualifiers.length == 0 ||
                                                Arrays.stream(qualifiers)
                                                        .anyMatch(qualifier -> qualifier.annotationType().getName().equals(entry.getKey()))
                                )
                );
            }

            @SuppressWarnings("unchecked")
            @Override
            public Stream<T> stream() {
                return instanceEntryStream.map(entry -> (T) entry.getValue());
            }
        };
    }

    public static <T> Instance<T> getInstance(T instance) {
        return getInstance(instance.getClass().getName(), instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> Instance<T> getInstance(String name, T instance) {
        return getInstance((Class<T>) instance.getClass(), name, instance);
    }

    public static <T> Mono<T> putIfAbsent(String requestId, Class<T> beanClass, T instance) {
        return putIfAbsent(requestId, beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings({"unchecked", "ReactiveStreamsNullableInLambdaInTransform"})
    public static <T> Mono<T> putIfAbsent(String requestId, Class<T> beanClass, String name, T instance) {
        return Mono.fromFuture(REQUEST_CACHE.get(requestId))
                .mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).putIfAbsent(name, instance));
    }

    public static <T> Mono<T> putIfAbsent(Class<T> beanClass, T instance) {
        return putIfAbsent(beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings({"unchecked", "ReactiveStreamsNullableInLambdaInTransform"})
    public static <T> Mono<T> putIfAbsent(Class<T> beanClass, String name, T instance) {
        return Mono.deferContextual(contextView -> Mono.fromFuture(REQUEST_CACHE.get(contextView.get(REQUEST_ID))))
                .mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).putIfAbsent(name, instance));
    }
}
