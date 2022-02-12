package io.graphoenix.core.context;

import io.graphoenix.spi.context.BeanProviders;
import io.graphoenix.spi.context.ModuleContext;
import jakarta.inject.Provider;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BeanContext {

    private static Set<ModuleContext> moduleContexts;

    private static final ClassValue<Map<String, Supplier<?>>> CONTEXT_CACHE = new BeanProviders();

    static {
        moduleContexts = ServiceLoader.load(ModuleContext.class, BeanContext.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
    }

    private BeanContext() {
    }

    public static void load(ClassLoader classLoader) {
        moduleContexts = ServiceLoader.load(ModuleContext.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
    }

    public static <T> T get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    public static <T> T get(Class<T> beanClass, String name) {
        Supplier<?> supplier = CONTEXT_CACHE.get(beanClass).get(name);
        if (supplier != null) {
            return beanClass.cast(supplier.get());
        }
        return getAndCache(beanClass, name);
    }

    public static <T> Provider<T> getProvider(Class<T> beanClass) {
        return getProvider(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Provider<T> getProvider(Class<T> beanClass, String name) {
        Supplier<?> supplier = CONTEXT_CACHE.get(beanClass).get(name);
        if (supplier != null) {
            return ((Supplier<T>) supplier)::get;
        }
        return getAndCacheProvider(beanClass, name);
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass) {
        return getOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass, String name) {
        Supplier<?> supplier = CONTEXT_CACHE.get(beanClass).get(name);
        if (supplier != null) {
            return Optional.of(beanClass.cast(supplier.get()));
        }
        return getAndCacheOptional(beanClass, name);
    }

    public static <T> Optional<Provider<T>> getProviderOptional(Class<T> beanClass) {
        return getProviderOptional(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<Provider<T>> getProviderOptional(Class<T> beanClass, String name) {
        Supplier<?> supplier = CONTEXT_CACHE.get(beanClass).get(name);
        if (supplier != null) {
            return Optional.of(((Supplier<T>) supplier)::get);
        }
        return getAndCacheProviderOptional(beanClass, name);
    }

    private static <T> T getAndCache(Class<T> beanClass, String name) {
        Supplier<T> cachedSupplier = getAndCacheSupplier(beanClass, name).orElseThrow();
        return beanClass.cast(cachedSupplier.get());
    }

    private static <T> Provider<T> getAndCacheProvider(Class<T> beanClass, String name) {
        Supplier<T> cachedSupplier = getAndCacheSupplier(beanClass, name).orElseThrow();
        return cachedSupplier::get;
    }

    private static <T> Optional<T> getAndCacheOptional(Class<T> beanClass, String name) {
        return getAndCacheSupplier(beanClass, name).map(Supplier::get);
    }

    private static <T> Optional<Provider<T>> getAndCacheProviderOptional(Class<T> beanClass, String name) {
        return getAndCacheSupplier(beanClass, name).map(supplier -> ((Supplier<T>) supplier)::get);
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Supplier<T>> getAndCacheSupplier(Class<T> beanClass, String name) {
        return moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(supplier -> (Supplier<T>) CONTEXT_CACHE.get(beanClass).putIfAbsent(name, supplier));
    }
}
