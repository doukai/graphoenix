package io.graphoenix.core.context;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import io.graphoenix.core.config.TimeoutConfig;
import io.graphoenix.spi.context.ScopeInstances;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class RequestCache {
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

    private RequestCache() {
    }

    public static <T> Mono<T> get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> get(Class<T> beanClass, String name) {
        return Mono.deferContextual(contextView -> Mono.fromFuture(REQUEST_CACHE.get(contextView.get(REQUEST_ID))))
                .mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).get(name));
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
