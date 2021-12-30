package io.graphoenix.core.utils;

import io.graphoenix.spi.handler.IBootstrapHandler;


public enum BootstrapHandlerUtil {
    BOOTSTRAP_HANDLER_UTIL;

    public IBootstrapHandler get(String className) {
        int lastIndexOf = className.lastIndexOf(".");
        String packageName = className.substring(0, lastIndexOf);
        String name = className.substring(lastIndexOf + 1);

        return null;
    }

    public <T> IBootstrapHandler get(Class<T> clazz) {
        return get(clazz.getName());
    }
}
