package io.graphoenix.spi.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class BaseModuleContext implements ModuleContext {

    private static final Map<Class<?>, Map<String, Supplier<?>>> contextMap = new HashMap<>();

    protected static void put(Class<?> beanClass, Supplier<?> supplier) {
        put(beanClass, beanClass.getName(), supplier);
    }

    protected static void put(Class<?> beanClass, String name, Supplier<?> supplier) {
        Map<String, Supplier<?>> supplierMap = contextMap.get(beanClass);
        if (supplierMap == null) {
            supplierMap = new HashMap<>();
        }
        supplierMap.put(name, supplier);
        contextMap.put(beanClass, supplierMap);
    }

    @Override
    public <T> Supplier<T> get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Supplier<T> get(Class<T> beanClass, String name) {
        return (Supplier<T>) contextMap.get(beanClass).get(name);
    }

    @Override
    public <T> Optional<Supplier<T>> getOptional(Class<T> beanClass) {
        return getOptional(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<Supplier<T>> getOptional(Class<T> beanClass, String name) {
        if (contextMap.get(beanClass) != null && contextMap.get(beanClass).get(name) != null) {
            return Optional.of((Supplier<T>) contextMap.get(beanClass).get(name));
        }
        return Optional.empty();
    }
}
