package io.graphoenix.spi.error;

import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class BaseHttpErrorStatus implements HttpErrorStatus {

    private final Map<Class<? extends Throwable>, Response.Status> statusMap = new HashMap<>();

    public BaseHttpErrorStatus() {
        register();
    }

    @Override
    public Optional<Response.Status> getStatus(Class<? extends Throwable> type) {
        if (statusMap.containsKey(type)) {
            return Optional.of(statusMap.get(type));
        }
        return Optional.empty();
    }

    protected void put(Class<? extends Throwable> type, Response.Status status) {
        statusMap.put(type, status);
    }

    public abstract void register();
}
