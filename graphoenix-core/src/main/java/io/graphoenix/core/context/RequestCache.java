package io.graphoenix.core.context;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import io.graphoenix.core.config.TimeoutConfig;
import io.graphoenix.spi.context.RequestInstances;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class RequestCache {
    public static final String REQUEST_ID = "requestId";

    private static final AsyncLoadingCache<String, RequestInstances> REQUEST_CACHE = buildCache();

    private static AsyncLoadingCache<String, RequestInstances> buildCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .evictionListener((key, value, cause) -> Logger.info("request id: {} eviction", key))
                .removalListener((key, value, cause) -> Logger.info("request id: {} removed", key));
        Config config = ConfigFactory.load();
        builder.expireAfterWrite(new TimeoutConfig().getRequest(), TimeUnit.SECONDS);
        if (config != null) {
            TimeoutConfig timeout = ConfigBeanFactory.create(config.getConfig("timeout"), TimeoutConfig.class);
            if (timeout != null) {
                builder.expireAfterWrite(timeout.getRequest(), TimeUnit.SECONDS);
            }
        }
        return builder.buildAsync(key -> new RequestInstances());
    }

    private RequestCache() {
    }

    public static <T> Mono<T> get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> get(Class<T> beanClass, String name) {
        return Mono.deferContextual(contextView -> Mono.fromFuture(REQUEST_CACHE.get(contextView.get(REQUEST_ID))))
                .mapNotNull(requestInstances -> (T) requestInstances.get(beanClass).get(name));
    }

    public static <T> Mono<T> putIfAbsent(String requestId, Class<T> beanClass, T instance) {
        return putIfAbsent(requestId, beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> putIfAbsent(String requestId, Class<T> beanClass, String name, T instance) {
        return Mono.fromFuture(REQUEST_CACHE.get(requestId))
                .mapNotNull(requestInstances -> (T) requestInstances.get(beanClass).putIfAbsent(name, instance));
    }

    public static <T> Mono<T> putIfAbsent(Class<T> beanClass, T instance) {
        return putIfAbsent(beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> putIfAbsent(Class<T> beanClass, String name, T instance) {
        return Mono.deferContextual(contextView -> Mono.fromFuture(REQUEST_CACHE.get(contextView.get(REQUEST_ID))))
                .mapNotNull(requestInstances -> (T) requestInstances.get(beanClass).putIfAbsent(name, instance));
    }
}
