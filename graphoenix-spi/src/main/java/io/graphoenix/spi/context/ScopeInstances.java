package io.graphoenix.spi.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScopeInstances extends ClassValue<Map<String, Object>> {

    private static final Map<String, Object> INSTANCE_MAP = new ConcurrentHashMap<>();

    @Override
    protected Map<String, Object> computeValue(Class<?> type) {
        return INSTANCE_MAP;
    }
}