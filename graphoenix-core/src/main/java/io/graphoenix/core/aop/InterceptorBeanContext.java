package io.graphoenix.core.aop;

import io.graphoenix.spi.aop.Interceptor;
import io.graphoenix.spi.aop.InterceptorBeanModuleContext;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InterceptorBeanContext {

    private static Set<InterceptorBeanModuleContext> moduleContexts;

    private static final Map<Class<? extends Annotation>, Supplier<? extends Interceptor>> contextCache = new HashMap<>();

    static {
        moduleContexts = ServiceLoader.load(InterceptorBeanModuleContext.class, InterceptorBeanContext.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
    }

    public static void load(ClassLoader classLoader) {
        moduleContexts = ServiceLoader.load(InterceptorBeanModuleContext.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation, R extends Interceptor> R get(Class<T> beanClass) {

        Supplier<R> supplier = (Supplier<R>) contextCache.get(beanClass);

        if (supplier != null) {
            return supplier.get();
        } else {
            return getAndCache(beanClass);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation, R extends Interceptor> Provider<R> getProvider(Class<T> beanClass) {

        Supplier<?> supplier = contextCache.get(beanClass);
        if (supplier != null) {
            return ((Supplier<R>) supplier)::get;
        } else {
            return getAndCacheProvider(beanClass);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation, R extends Interceptor> R getAndCache(Class<T> beanClass) {

        Supplier<R> supplier = (Supplier<R>) moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow();

        contextCache.put(beanClass, supplier);
        return supplier.get();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation, R extends Interceptor> Provider<R> getAndCacheProvider(Class<T> beanClass) {

        Supplier<R> supplier = (Supplier<R>) moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow();

        contextCache.put(beanClass, supplier);
        return supplier::get;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation, R extends Interceptor> Optional<R> getOptional(Class<T> beanClass) {

        Supplier<R> supplier = (Supplier<R>) contextCache.get(beanClass);

        if (supplier != null) {
            return Optional.of(supplier.get());
        } else {
            return getAndCacheOptional(beanClass);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation, R extends Interceptor> Optional<R> getAndCacheOptional(Class<T> beanClass) {

        Optional<? extends Supplier<? extends Interceptor>> supplierOptional = moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        supplierOptional.ifPresent(supplier -> contextCache.put(beanClass, supplier));

        return (Optional<R>) supplierOptional.map(Supplier::get);
    }
}
