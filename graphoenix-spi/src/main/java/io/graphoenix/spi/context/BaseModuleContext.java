package io.graphoenix.spi.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class BaseModuleContext implements ModuleContext {

    private static final Map<Class<?>, Supplier<?>> contextMap = new HashMap<>();

    protected static void put(Class<?> beanClass, Supplier<?> supplier) {
        contextMap.put(beanClass, supplier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Supplier<T> get(Class<T> beanClass) {
        return (Supplier<T>) contextMap.get(beanClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<Supplier<T>> getOptional(Class<T> beanClass) {
        if (contextMap.get(beanClass) != null) {
            return Optional.of((Supplier<T>) contextMap.get(beanClass));
        }
        Optional<Map.Entry<Class<?>, Supplier<?>>> subType = contextMap.entrySet().stream()
                .filter(classSupplierEntry -> classSupplierEntry.getKey().isAssignableFrom(beanClass))
                .findFirst();

        return subType.map(classSupplierEntry -> (Supplier<T>) classSupplierEntry.getValue());
    }
}
