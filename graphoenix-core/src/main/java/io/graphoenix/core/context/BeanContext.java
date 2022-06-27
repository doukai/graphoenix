package io.graphoenix.core.context;

import io.graphoenix.spi.context.BeanProviders;
import io.graphoenix.spi.context.ModuleContext;
import jakarta.inject.Provider;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BeanContext {

    private static Set<ModuleContext> moduleContexts;

    private static final BeanProviders CONTEXT_CACHE = new BeanProviders();

    static {
        Logger.info("load ModuleContext from {}", BeanContext.class.getClassLoader().getName());
        moduleContexts = ServiceLoader.load(ModuleContext.class, BeanContext.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
        Logger.info(moduleContexts.size() + " ModuleContext loaded");
    }

    private BeanContext() {
    }

    public static void load(ClassLoader classLoader) {
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

    private static <T> Supplier<T> getSupplier(Class<T> beanClass, String name) {
        return getSupplierOptional(beanClass, name)
                .orElseGet(() -> getAndCacheSupplier(beanClass, name).orElse(null));
    }

    private static <T> Supplier<Mono<T>> getMonoSupplier(Class<T> beanClass, String name) {
        return getMonoSupplierOptional(beanClass, name)
                .orElseGet(() -> getAndCacheMonoSupplier(beanClass, name).orElse(null));
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
    private static <T> Optional<Supplier<T>> getAndCacheSupplier(Class<T> beanClass, String name) {
        Logger.debug("search bean instance for class {} name {}", beanClass.getName(), name);
        return moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(supplier -> (Supplier<T>) CONTEXT_CACHE.get(beanClass).putIfAbsent(name, supplier));
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Supplier<Mono<T>>> getAndCacheMonoSupplier(Class<T> beanClass, String name) {
        Logger.debug("search bean instance for class {} name {}", beanClass.getName(), name);
        return moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(supplier -> (Supplier<Mono<T>>) CONTEXT_CACHE.get(beanClass).putIfAbsent(name, supplier));
    }
}
