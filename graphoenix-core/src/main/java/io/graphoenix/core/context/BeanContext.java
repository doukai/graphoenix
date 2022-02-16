package io.graphoenix.core.context;

import io.graphoenix.core.error.InjectionProblem;
import io.graphoenix.spi.context.BeanProviders;
import io.graphoenix.spi.context.ModuleContext;
import jakarta.inject.Provider;
import org.tinylog.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.graphoenix.spi.error.InjectionErrorType.BEAN_NOT_EXIST;

public class BeanContext {

    private static Set<ModuleContext> moduleContexts;

    private static final ClassValue<Map<String, Supplier<?>>> CONTEXT_CACHE = new BeanProviders();

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

    public static <T> Provider<T> getProvider(Class<T> beanClass) {
        return getProvider(beanClass, beanClass.getName());
    }

    public static <T> Provider<T> getProvider(Class<T> beanClass, String name) {
        return getSupplier(beanClass, name)::get;
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass) {
        return getOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<T> getOptional(Class<T> beanClass, String name) {
        return getSupplierOptional(beanClass, name).map(Supplier::get);
    }

    public static <T> Optional<Provider<T>> getProviderOptional(Class<T> beanClass) {
        return getProviderOptional(beanClass, beanClass.getName());
    }

    public static <T> Optional<Provider<T>> getProviderOptional(Class<T> beanClass, String name) {
        return getSupplierOptional(beanClass, name).map(supplier -> supplier::get);
    }

    private static <T> Supplier<T> getSupplier(Class<T> beanClass, String name) {
        return getSupplierOptional(beanClass, name)
                .orElseGet(() ->
                        getAndCacheSupplier(beanClass, name)
                                .orElseThrow(() -> new InjectionProblem(BEAN_NOT_EXIST.bind(beanClass, name)))
                );
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
    private static <T> Optional<Supplier<T>> getAndCacheSupplier(Class<T> beanClass, String name) {
        Logger.debug("search bean instance for class {} name {}", beanClass.getName(), name);
        return moduleContexts.stream()
                .map(moduleContext -> moduleContext.getOptional(beanClass, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(supplier -> (Supplier<T>) CONTEXT_CACHE.get(beanClass).putIfAbsent(name, supplier));
    }
}
