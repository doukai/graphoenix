package io.graphoenix.core.utils;

import io.graphoenix.spi.handler.IOperationHandler;

public enum OperationHandlerUtil {
    OPERATION_HANDLER_UTIL;

    public IOperationHandler get(String className) {
        int lastIndexOf = className.lastIndexOf(".");
        String packageName = className.substring(0, lastIndexOf);
        String name = className.substring(lastIndexOf + 1);

        return null;
    }

    public <T> IOperationHandler get(Class<T> clazz) {
        return get(clazz.getName());
    }
}
