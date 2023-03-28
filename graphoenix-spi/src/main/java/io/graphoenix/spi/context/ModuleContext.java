package io.graphoenix.spi.context;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface ModuleContext {

    <T> Supplier<T> get(Class<T> beanClass);

    <T> Supplier<T> get(Class<T> beanClass, String name);

    <T> Optional<Supplier<T>> getOptional(Class<T> beanClass);

    <T> Optional<Supplier<T>> getOptional(Class<T> beanClass, String name);

    @SuppressWarnings("unchecked")
    <T> Map<String, T> getMap(Class<T> beanClass);

    @SuppressWarnings("unchecked")
    <T> Map<String, Supplier<?>> getSupplierMap(Class<T> beanClass);
}
