package io.graphoenix.core.context;

import io.graphoenix.spi.context.ModuleContext;

import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BeanContext {

    private static Set<ModuleContext> moduleContexts;

    private static final Map<Class<?>, Map<String, Supplier<?>>> contextCache = new ConcurrentHashMap<>();

    static {
        moduleContexts = ServiceLoader.load(ModuleContext.class, BeanContext.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
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
        Map<String, Supplier<?>> supplierMap = contextCache.get(beanClass);
        if (supplierMap != null) {
            Supplier<?> supplier = supplierMap.get(name);
            if (supplier != null) {
                return beanClass.cast(supplier.get());
            }
        }
        return getAndCache(beanClass, name);
    }

    public static <T> Provider<T> getProvider(Class<T> beanClass) {
        return getProvider(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> Provider<T> getProvider(Class<T> beanClass, String name) {
        Map<String, Supplier<?>> supplierMap = contextCache.get(beanClass);
        if (supplierMap != null) {
            Supplier<?> supplier = supplierMap.get(name);
            if (supplier != null) {
                return ((Supplier<T>) supplier)::get;
            }
        }
        return getAndCacheProvider(beanClass, name);
    }

    private static <T> T getAndCache(Class<T> beanClass, String name) {
        Supplier<T> supplier = moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow();

        Map<String, Supplier<?>> supplierMap = contextCache.get(beanClass);
        if (supplierMap == null) {
            supplierMap = new ConcurrentHashMap<>();
        }
        supplierMap.put(name, supplier);
        contextCache.put(beanClass, supplierMap);
        return beanClass.cast(supplier.get());
    }

    private static <T> Provider<T> getAndCacheProvider(Class<T> beanClass, String name) {
        Supplier<T> supplier = moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow();

        Map<String, Supplier<?>> supplierMap = contextCache.get(beanClass);
        if (supplierMap == null) {
            supplierMap = new ConcurrentHashMap<>();
        }
        supplierMap.put(name, supplier);
        contextCache.put(beanClass, supplierMap);
        return supplier::get;
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass) {
        return getOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass, String name) {
        Map<String, Supplier<?>> supplierMap = contextCache.get(beanClass);
        if (supplierMap != null) {
            Supplier<?> supplier = supplierMap.get(name);
            if (supplier != null) {
                return Optional.of(beanClass.cast(supplier.get()));
            }
        }
        return getAndCacheOptional(beanClass, name);
    }

    private static <T> Optional<T> getAndCacheOptional(Class<T> beanClass, String name) {
        Optional<Supplier<T>> supplierOptional = moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        supplierOptional.ifPresent(supplier -> {
                    Map<String, Supplier<?>> supplierMap = contextCache.get(beanClass);
                    if (supplierMap == null) {
                        supplierMap = new ConcurrentHashMap<>();
                    }
                    supplierMap.put(name, supplier);
                    contextCache.put(beanClass, supplierMap);
                }
        );
        return supplierOptional.map(Supplier::get);
    }
}
