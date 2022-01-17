package io.graphoenix.spi.handler;

import java.util.Map;
import java.util.function.Function;

public abstract class BaseQueryHandler implements QueryHandler {

    private Map<String, Function<Map<String, Object>, ?>> invokeFunctions;

    @SuppressWarnings("unchecked")
    @Override
    public <T> Function<Map<String, Object>, T> getInvokeMethod(String name) {
        return (Function<Map<String, Object>, T>) invokeFunctions.get(name);
    }

    protected void put(String name, Function<Map<String, Object>, ?> function) {
        invokeFunctions.put(name, function);
    }
}
