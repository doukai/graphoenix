package io.graphoenix.spi.error;

import java.util.Optional;

public interface ErrorInfo {

    Optional<Integer> getCode(Class<? extends Throwable> type);

    Optional<String> getMessage(Class<? extends Throwable> type);
}
