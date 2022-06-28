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

public class SessionCache {
    public static final String SESSION_ID = "sessionId";

    private static final AsyncLoadingCache<String, ScopeInstances> SESSION_CACHE = buildCache();

    private static AsyncLoadingCache<String, ScopeInstances> buildCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .evictionListener((key, value, cause) -> Logger.info("session id: {} eviction", key))
                .removalListener((key, value, cause) -> Logger.info("session id: {} removed", key));
        Config config = ConfigFactory.load();
        builder.expireAfterWrite(new TimeoutConfig().getSession(), TimeUnit.SECONDS);
        if (config != null) {
            TimeoutConfig timeout = ConfigBeanFactory.create(config.getConfig("timeout"), TimeoutConfig.class);
            if (timeout != null) {
                builder.expireAfterWrite(timeout.getSession(), TimeUnit.SECONDS);
            }
        }
        return builder.buildAsync(key -> new ScopeInstances());
    }

    private SessionCache() {
    }

    public static <T> Mono<T> get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> get(Class<T> beanClass, String name) {
        return Mono.deferContextual(contextView -> Mono.fromFuture(SESSION_CACHE.get(contextView.get(SESSION_ID))))
                .mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).get(name));
    }

    public static <T> Mono<T> putIfAbsent(String sessionId, Class<T> beanClass, T instance) {
        return putIfAbsent(sessionId, beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings({"unchecked", "ReactiveStreamsNullableInLambdaInTransform"})
    public static <T> Mono<T> putIfAbsent(String sessionId, Class<T> beanClass, String name, T instance) {
        return Mono.fromFuture(SESSION_CACHE.get(sessionId))
                .mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).putIfAbsent(name, instance));
    }

    public static <T> Mono<T> putIfAbsent(Class<T> beanClass, T instance) {
        return putIfAbsent(beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings({"unchecked", "ReactiveStreamsNullableInLambdaInTransform"})
    public static <T> Mono<T> putIfAbsent(Class<T> beanClass, String name, T instance) {
        return Mono.deferContextual(contextView -> Mono.fromFuture(SESSION_CACHE.get(contextView.get(SESSION_ID))))
                .mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).putIfAbsent(name, instance));
    }
}
