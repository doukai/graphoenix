package io.graphoenix.spi.error;

import jakarta.ws.rs.core.Response;

import java.util.Optional;

public interface HttpErrorStatus {

    Optional<Response.Status> getStatus(Class<? extends Throwable> type);
}
