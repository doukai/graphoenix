package io.graphoenix.spi.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class BeanProviders extends ClassValue<Map<String, Supplier<?>>> {

    private static final Map<String, Supplier<?>> SUPPLIER_MAP = new ConcurrentHashMap<>();

    @Override
    protected Map<String, Supplier<?>> computeValue(Class<?> type) {
        return SUPPLIER_MAP;
    }
}