package io.graphoenix.spi.context;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class BaseModuleContext implements ModuleContext {

    private static final ClassValue<Map<String, Supplier<?>>> CONTEXT = new BeanProviders();

    protected static void put(Class<?> beanClass, Supplier<?> supplier) {
        put(beanClass, beanClass.getName(), supplier);
    }

    protected static void put(Class<?> beanClass, String name, Supplier<?> supplier) {
        CONTEXT.get(beanClass).put(name, supplier);
    }

    @Override
    public <T> Supplier<T> get(Class<T> beanClass) {
        return get(beanClass, beanClass.getName());
    }

    @Override
    public <T> Supplier<T> get(Class<T> beanClass, String name) {
        return getOptional(beanClass, name).orElseThrow(() -> new RuntimeException(beanClass.getName().concat(" not found")));
    }

    @Override
    public <T> Optional<Supplier<T>> getOptional(Class<T> beanClass) {
        return getOptional(beanClass, beanClass.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<Supplier<T>> getOptional(Class<T> beanClass, String name) {
        if (CONTEXT.get(beanClass).get(name) != null) {
            return Optional.of((Supplier<T>) CONTEXT.get(beanClass).get(name));
        }
        return Optional.empty();
    }
}
