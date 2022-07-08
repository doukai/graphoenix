package io.graphoenix.core.context;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import io.graphoenix.core.config.TimeoutConfig;
import io.graphoenix.spi.context.ScopeInstances;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class RequestPublisherBuilderFactory {
    public static final String REQUEST_ID = "requestId";

    private static final AsyncLoadingCache<String, ScopeInstances> REQUEST_CACHE = buildCache();
    private static final ReactiveStreamsFactory reactiveStreamsFactory = ReactiveStreamsFactoryResolver.instance();

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

    private RequestPublisherBuilderFactory() {
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

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass) {
        return getPublisherBuilder(beanClass, beanClass.getName());
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, String name) {
        return reactiveStreamsFactory.fromPublisher(get(beanClass, name));
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, T instance) {
        return getPublisherBuilder(beanClass, beanClass.getName(), instance);
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, String name, T instance) {
        return reactiveStreamsFactory.fromPublisher(get(beanClass, name, instance));
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(T instance) {
        return getPublisherBuilder(instance.getClass().getName(), instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> PublisherBuilder<T> getPublisherBuilder(String name, T instance) {
        return getPublisherBuilder((Class<T>) instance.getClass(), name, instance);
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
