package io.graphoenix.core.context;

import io.graphoenix.spi.context.ModuleContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BeanContext {

    private static Set<ModuleContext> moduleContexts;

    private static final Map<Class<?>, Supplier<?>> contextCache = new HashMap<>();

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

        Supplier<?> supplier = contextCache.get(beanClass);

        if (supplier != null) {
            return beanClass.cast(supplier.get());
        } else {
            return getAndCache(beanClass);
        }
    }

    private static <T> T getAndCache(Class<T> beanClass) {

        Supplier<T> supplier = moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow();

        contextCache.put(beanClass, supplier);
        return beanClass.cast(supplier.get());
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass) {

        Supplier<?> supplier = contextCache.get(beanClass);

        if (supplier != null) {
            return Optional.of(beanClass.cast(supplier.get()));
        } else {
            return getAndCacheOptional(beanClass);
        }
    }


    private static <T> Optional<T> getAndCacheOptional(Class<T> beanClass) {

        Optional<Supplier<T>> supplierOptional = moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        supplierOptional.ifPresent(supplier -> contextCache.put(beanClass, supplier));

        return supplierOptional.map(Supplier::get);
    }
}
