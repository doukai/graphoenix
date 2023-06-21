package io.graphoenix.core.context;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.graphoenix.core.config.TimeoutConfig;
import io.graphoenix.spi.context.ScopeInstances;
import jakarta.inject.Provider;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

import static io.graphoenix.spi.constant.Hammurabi.TRANSACTION_ID;

public class TransactionScopeInstanceFactory {

    private static final AsyncLoadingCache<String, ScopeInstances> TRANSACTION_CACHE = buildCache();
    private static final ReactiveStreamsFactory reactiveStreamsFactory = BeanContext.get(ReactiveStreamsFactory.class);

    private static AsyncLoadingCache<String, ScopeInstances> buildCache() {
        TimeoutConfig timeoutConfig = BeanContext.getOptional(TimeoutConfig.class).orElseGet(TimeoutConfig::new);
        return Caffeine.newBuilder()
                .expireAfterAccess(timeoutConfig.getTransaction(), TimeUnit.SECONDS)
                .evictionListener((key, value, cause) -> Logger.info("transaction id: {} eviction", key))
                .removalListener((key, value, cause) -> Logger.info("transaction id: {} removed", key))
                .buildAsync(key -> new ScopeInstances());
    }

    private TransactionScopeInstanceFactory() {
    }

    public static Mono<ScopeInstances> getScopeInstances() {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(TRANSACTION_ID)).flatMap(id -> Mono.fromFuture(TRANSACTION_CACHE.get((String) id))));
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

    @SuppressWarnings({"unchecked"})
    public static <T> Mono<T> get(Class<T> beanClass, String name, Provider<T> instanceProvider) {
        return get(beanClass, name).switchIfEmpty(getScopeInstances().mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).computeIfAbsent(name, key -> instanceProvider.get())));
    }

    public static <T> Mono<T> getByMonoProvider(Class<T> beanClass, Provider<Mono<T>> instanceMonoProvider) {
        return getByMonoProvider(beanClass, beanClass.getName(), instanceMonoProvider);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Mono<T> getByMonoProvider(Class<T> beanClass, String name, Provider<Mono<T>> instanceMonoProvider) {
        return get(beanClass, name).switchIfEmpty(getScopeInstances().flatMap(scopeInstances -> instanceMonoProvider.get().mapNotNull(instance -> (T) scopeInstances.get(beanClass).computeIfAbsent(name, key -> instance))));
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

    public static <T> PublisherBuilder<T> getPublisherBuilderByMonoProvider(Class<T> beanClass, Provider<Mono<T>> instanceMonoProvider) {
        return getPublisherBuilderByMonoProvider(beanClass, beanClass.getName(), instanceMonoProvider);
    }

    public static <T> PublisherBuilder<T> getPublisherBuilderByMonoProvider(Class<T> beanClass, String name, Provider<Mono<T>> instanceMonoProvider) {
        return reactiveStreamsFactory.fromPublisher(getByMonoProvider(beanClass, name, instanceMonoProvider));
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> putIfAbsent(T instance) {
        return putIfAbsent((Class<T>) instance.getClass(), instance);
    }

    public static <T, E extends T> Mono<T> putIfAbsent(Class<T> beanClass, E instance) {
        return putIfAbsent(beanClass, beanClass.getName(), instance);
    }

    @SuppressWarnings("unchecked")
    public static <T, E extends T> Mono<T> putIfAbsent(Class<T> beanClass, String name, E instance) {
        return getScopeInstances().mapNotNull(scopeInstances -> (T) scopeInstances.get(beanClass).putIfAbsent(name, instance));
    }
}
