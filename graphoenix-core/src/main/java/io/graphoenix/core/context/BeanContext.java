package io.graphoenix.core.context;

import io.graphoenix.spi.context.BeanProviders;
import io.graphoenix.spi.context.ModuleContext;
import jakarta.inject.Provider;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BeanContext {

    private static Set<ModuleContext> moduleContexts;

    private static final BeanProviders CONTEXT_CACHE = new BeanProviders();

    static {
        load(BeanContext.class.getClassLoader());
    }

    private BeanContext() {
    }

    public static void load(ClassLoader classLoader) {
        Thread.currentThread().setContextClassLoader(classLoader);
        Logger.info("load ModuleContext from {}", classLoader.getName());
        moduleContexts = ServiceLoader.load(ModuleContext.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
        Logger.info(moduleContexts.size() + " ModuleContext loaded");
    }

    public static <T> T get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    public static <T> T get(Class<T> beanClass, String name) {
        return getSupplier(beanClass, name).get();
    }

    public static <T> Mono<T> getMono(Class<T> beanClass) {
        return getMono(beanClass, beanClass.getName());
    }

    public static <T> Mono<T> getMono(Class<T> beanClass, String name) {
        return getMonoSupplier(beanClass, name).get();
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass) {
        return getPublisherBuilder(beanClass, beanClass.getName());
    }

    public static <T> PublisherBuilder<T> getPublisherBuilder(Class<T> beanClass, String name) {
        return getPublisherBuilderSupplier(beanClass, name).get();
    }

    public static <T> Provider<T> getProvider(Class<T> beanClass) {
        return getProvider(beanClass, beanClass.getName());
    }

    public static <T> Provider<T> getProvider(Class<T> beanClass, String name) {
        return getSupplier(beanClass, name)::get;
    }

    public static <T> Provider<Mono<T>> getMonoProvider(Class<T> beanClass) {
        return getMonoProvider(beanClass, beanClass.getName());
    }

    public static <T> Provider<Mono<T>> getMonoProvider(Class<T> beanClass, String name) {
        return getMonoSupplier(beanClass, name)::get;
    }

    public static <T> Provider<PublisherBuilder<T>> getPublisherBuilderProvider(Class<T> beanClass) {
        return getPublisherBuilderProvider(beanClass, beanClass.getName());
    }

    public static <T> Provider<PublisherBuilder<T>> getPublisherBuilderProvider(Class<T> beanClass, String name) {
        return getPublisherBuilderSupplier(beanClass, name)::get;
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass) {
        return getOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass, String name) {
        return getSupplierOptional(beanClass, name).map(Supplier::get);
    }

    public static <T> Optional<Mono<T>> getMonoOptional(Class<T> beanClass) {
        return getMonoOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<Mono<T>> getMonoOptional(Class<T> beanClass, String name) {
        return getMonoSupplierOptional(beanClass, name).map(Supplier::get);
    }

    public static <T> Optional<PublisherBuilder<T>> getPublisherBuilderOptional(Class<T> beanClass) {
        return getPublisherBuilderOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<PublisherBuilder<T>> getPublisherBuilderOptional(Class<T> beanClass, String name) {
        return getPublisherBuilderSupplierOptional(beanClass, name).map(Supplier::get);
    }

    public static <T> Optional<Provider<T>> getProviderOptional(Class<T> beanClass) {
        return getProviderOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<Provider<T>> getProviderOptional(Class<T> beanClass, String name) {
        return getSupplierOptional(beanClass, name).map(supplier -> supplier::get);
    }

    public static <T> Optional<Provider<Mono<T>>> getMonoProviderOptional(Class<T> beanClass) {
        return getMonoProviderOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<Provider<Mono<T>>> getMonoProviderOptional(Class<T> beanClass, String name) {
        return getMonoSupplierOptional(beanClass, name).map(supplier -> supplier::get);
    }

    public static <T> Optional<Provider<PublisherBuilder<T>>> getPublisherBuilderProviderOptional(Class<T> beanClass) {
        return getPublisherBuilderProviderOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<Provider<PublisherBuilder<T>>> getPublisherBuilderProviderOptional(Class<T> beanClass, String name) {
        return getPublisherBuilderSupplierOptional(beanClass, name).map(supplier -> supplier::get);
    }

    private static <T> Supplier<T> getSupplier(Class<T> beanClass, String name) {
        return getSupplierOptional(beanClass, name)
                .orElseGet(() -> getAndCacheSupplier(beanClass, name).orElse(null));
    }

    private static <T> Supplier<Mono<T>> getMonoSupplier(Class<T> beanClass, String name) {
        return getMonoSupplierOptional(beanClass, name)
                .orElseGet(() -> getAndCacheMonoSupplier(beanClass, name).orElse(null));
    }

    private static <T> Supplier<PublisherBuilder<T>> getPublisherBuilderSupplier(Class<T> beanClass, String name) {
        return getPublisherBuilderSupplierOptional(beanClass, name)
                .orElseGet(() -> getAndCachePublisherBuilderSupplier(beanClass, name).orElse(null));
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Supplier<T>> getSupplierOptional(Class<T> beanClass, String name) {
        Supplier<?> supplier = CONTEXT_CACHE.get(beanClass).get(name);
        if (supplier != null) {
            return Optional.of((Supplier<T>) supplier);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Supplier<Mono<T>>> getMonoSupplierOptional(Class<T> beanClass, String name) {
        Supplier<?> supplier = CONTEXT_CACHE.get(beanClass).get(name);
        if (supplier != null) {
            return Optional.of((Supplier<Mono<T>>) supplier);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Supplier<PublisherBuilder<T>>> getPublisherBuilderSupplierOptional(Class<T> beanClass, String name) {
        Supplier<?> supplier = CONTEXT_CACHE.get(beanClass).get(name);
        if (supplier != null) {
            return Optional.of(() -> ReactiveStreamsFactoryResolver.instance().fromPublisher(((Supplier<Mono<T>>) supplier).get()));
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Supplier<T>> getAndCacheSupplier(Class<T> beanClass, String name) {
        Logger.debug("search bean instance for class {} name {}", beanClass.getName(), name);
        return moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(supplier -> (Supplier<T>) CONTEXT_CACHE.get(beanClass).computeIfAbsent(name, k -> supplier));
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Supplier<Mono<T>>> getAndCacheMonoSupplier(Class<T> beanClass, String name) {
        Logger.debug("search bean instance for class {} name {}", beanClass.getName(), name);
        return moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(supplier -> (Supplier<Mono<T>>) CONTEXT_CACHE.get(beanClass).computeIfAbsent(name, k -> supplier));
    }

    private static <T> Optional<Supplier<PublisherBuilder<T>>> getAndCachePublisherBuilderSupplier(Class<T> beanClass, String name) {
        Logger.debug("search bean instance for class {} name {}", beanClass.getName(), name);
        return getAndCacheMonoSupplier(beanClass, name)
                .map(monoSupplier -> () -> ReactiveStreamsFactoryResolver.instance().fromPublisher(monoSupplier.get()));
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> getMap(Class<T> beanClass) {
        Logger.debug("search bean map for class {}", beanClass.getName());
        return moduleContexts.stream()
                .flatMap(moduleContext ->
                        Stream.ofNullable(moduleContext.getSupplierMap(beanClass))
                                .flatMap(map -> map.entrySet().stream())
                                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (T) entry.getValue().get()))
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, Mono<T>> getMonoMap(Class<T> beanClass) {
        Logger.debug("search bean map for class {}", beanClass.getName());
        return moduleContexts.stream()
                .flatMap(moduleContext ->
                        Stream.ofNullable(moduleContext.getSupplierMap(beanClass))
                                .flatMap(map -> map.entrySet().stream())
                                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (Mono<T>) entry.getValue().get()))
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, PublisherBuilder<T>> getPublisherBuilderMap(Class<T> beanClass) {
        Logger.debug("search bean map for class {}", beanClass.getName());
        return moduleContexts.stream()
                .flatMap(moduleContext ->
                        Stream.ofNullable(moduleContext.getSupplierMap(beanClass))
                                .flatMap(map -> map.entrySet().stream())
                                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), ReactiveStreamsFactoryResolver.instance().fromPublisher((Mono<T>) entry.getValue().get())))
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, Supplier<T>> getSupplierMap(Class<T> beanClass) {
        Logger.debug("search bean map for class {}", beanClass.getName());
        return moduleContexts.stream()
                .flatMap(moduleContext ->
                        Stream.ofNullable(moduleContext.getSupplierMap(beanClass))
                                .flatMap(map -> map.entrySet().stream())
                                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (Supplier<T>) entry.getValue()))
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, Supplier<Mono<T>>> getMonoSupplierMap(Class<T> beanClass) {
        Logger.debug("search bean map for class {}", beanClass.getName());
        return moduleContexts.stream()
                .flatMap(moduleContext ->
                        Stream.ofNullable(moduleContext.getSupplierMap(beanClass))
                                .flatMap(map -> map.entrySet().stream())
                                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (Supplier<Mono<T>>) entry.getValue()))
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, Supplier<PublisherBuilder<T>>> getPublisherBuilderSupplierMap(Class<T> beanClass) {
        Logger.debug("search bean map for class {}", beanClass.getName());
        return moduleContexts.stream()
                .flatMap(moduleContext ->
                        Stream.ofNullable(moduleContext.getSupplierMap(beanClass))
                                .flatMap(map -> map.entrySet().stream())
                                .map(entry -> {
                                            Supplier<PublisherBuilder<T>> publisherBuilderSupplier = () -> ReactiveStreamsFactoryResolver.instance().fromPublisher((Mono<T>) entry.getValue().get());
                                            return new AbstractMap.SimpleEntry<>(entry.getKey(), publisherBuilderSupplier);
                                        }
                                )
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
