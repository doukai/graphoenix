package io.graphoenix.spi.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class BaseHttpErrorStatus implements HttpErrorStatus {

    private final Map<Class<? extends Throwable>, Integer> statusMap = new HashMap<>();

    public BaseHttpErrorStatus() {
        register();
    }

    @Override
    public Optional<Integer> getStatus(Class<? extends Throwable> type) {
        if (statusMap.containsKey(type)) {
            return Optional.of(statusMap.get(type));
        }
        return Optional.empty();
    }

    protected void put(Class<? extends Throwable> type, Integer status) {
        statusMap.put(type, status);
    }

    public abstract void register();
}
