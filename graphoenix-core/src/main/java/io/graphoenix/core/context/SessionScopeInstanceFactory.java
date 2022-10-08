package io.graphoenix.core.context;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import io.graphoenix.core.config.TimeoutConfig;
import io.graphoenix.spi.context.ScopeInstances;
import jakarta.inject.Provider;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

import static io.graphoenix.spi.constant.Hammurabi.SESSION_ID;

public class SessionScopeInstanceFactory {

    private static final AsyncLoadingCache<String, ScopeInstances> SESSION_CACHE = buildCache();
    private static final ReactiveStreamsFactory reactiveStreamsFactory = BeanContext.get(ReactiveStreamsFactory.class);

    private static AsyncLoadingCache<String, ScopeInstances> buildCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .evictionListener((key, value, cause) -> Logger.info("session id: {} eviction", key))
                .removalListener((key, value, cause) -> Logger.info("session id: {} removed", key));
        Config config = ConfigFactory.load();
        if (config != null && config.hasPath("timeout")) {
            TimeoutConfig timeout = ConfigBeanFactory.create(config.getConfig("timeout"), TimeoutConfig.class);
            builder.expireAfterWrite(timeout.getSession(), TimeUnit.SECONDS);
        } else {
            builder.expireAfterWrite(new TimeoutConfig().getSession(), TimeUnit.SECONDS);
        }
        return builder.buildAsync(key -> new ScopeInstances());
    }

    private SessionScopeInstanceFactory() {
    }

    public static Mono<ScopeInstances> getScopeInstances() {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(SESSION_ID)).flatMap(id -> Mono.fromFuture(SESSION_CACHE.get((String) id))));
    }

    public static <T> Mono<T> get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> get(Class<T> beanClass, String name) {
        return getScopeInstances().mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).get(name));
    }

    public static <T> Mono<T> get(Class<T> beanClass, Provider<T> instanceProvider) {
        return get(beanClass, beanClass.getName(), instanceProvider);
    }

    @SuppressWarnings({"unchecked", "ReactiveStreamsNullableInLambdaInTransform"})
    public static <T> Mono<T> get(Class<T> beanClass, String name, Provider<T> instanceProvider) {
        return get(beanClass, name).switchIfEmpty(getScopeInstances().map(scopeInstances -> (T) scopeInstances.get(beanClass).putIfAbsent(name, instanceProvider.get())));
    }

    public static <T> Mono<T> get(Class<T> beanClass, Mono<T> instanceMono) {
        return get(beanClass, beanClass.getName(), instanceMono);
    }

    @SuppressWarnings({"unchecked", "ReactiveStreamsNullableInLambdaInTransform"})
    public static <T> Mono<T> get(Class<T> beanClass, String name, Mono<T> instanceMono) {
        return get(beanClass, name).switchIfEmpty(instanceMono.flatMap(instance -> getScopeInstances().map(scopeInstances -> (T) scopeInstances.get(beanClass).putIfAbsent(name, instance))));
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass) {
        return getPublisherBuilder(beanClass, beanClass.getName());
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, String name) {
        return reactiveStreamsFactory.fromPublisher(get(beanClass, name));
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, Provider<T> instanceProvider) {
        return getPublisherBuilder(beanClass, beanClass.getName(), instanceProvider);
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, String name, Provider<T> instanceProvider) {
        return reactiveStreamsFactory.fromPublisher(get(beanClass, name, instanceProvider));
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, Mono<T> instanceMono) {
        return getPublisherBuilder(beanClass, beanClass.getName(), instanceMono);
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, String name, Mono<T> instanceMono) {
        return reactiveStreamsFactory.fromPublisher(get(beanClass, name, instanceMono));
    }
}
