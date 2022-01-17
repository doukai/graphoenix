package io.graphoenix.spi.handler;

import java.util.Map;
import java.util.function.Function;

public class BaseInvokeHandler implements InvokeHandler {

    private Map<Class<?>, Function<?, ?>> invokeFunctions;

    @SuppressWarnings("unchecked")
    @Override
    public <T> Function<T, T> getInvokeMethod(Class<T> beanCLass) {
        return (Function<T, T>) invokeFunctions.get(beanCLass);
    }

    protected void put(Class<?> beanClass, Function<?, ?> function) {
        invokeFunctions.put(beanClass, function);
    }
}
