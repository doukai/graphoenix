package io.graphoenix.spi.handler;

import java.util.function.Function;

public interface InvokeHandler {

    <T> Function<T, ? extends T> getInvokeMethod(Class<T> beanCLass);
}
