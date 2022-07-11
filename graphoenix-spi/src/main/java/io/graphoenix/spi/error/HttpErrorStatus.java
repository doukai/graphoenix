package io.graphoenix.spi.error;

import java.util.Optional;

public interface HttpErrorStatus {

    Optional<Integer> getStatus(Class<? extends Throwable> type);
}
