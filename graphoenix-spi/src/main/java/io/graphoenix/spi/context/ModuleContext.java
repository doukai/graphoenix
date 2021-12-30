package io.graphoenix.spi.context;

import java.util.Optional;
import java.util.function.Supplier;

public interface ModuleContext {

    <T> Supplier<T> get(Class<T> beanClass);

    <T> Optional<Supplier<T>> getOptional(Class<T> beanClass);
}
