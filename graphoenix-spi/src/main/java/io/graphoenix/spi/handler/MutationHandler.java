package io.graphoenix.spi.handler;

import java.util.Map;
import java.util.function.Function;

public interface MutationHandler {

    <T> Function<Map<String, Object>, T> getInvokeMethod(String name);
}
