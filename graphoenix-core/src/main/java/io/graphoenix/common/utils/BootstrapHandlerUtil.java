package io.graphoenix.common.utils;

import io.graphoenix.spi.handler.IBootstrapHandler;

import static org.joor.Reflect.*;

public enum BootstrapHandlerUtil {

    BOOTSTRAP_HANDLER_UTIL;

    public IBootstrapHandler get(String className) {
        int lastIndexOf = className.lastIndexOf(".");
        String packageName = className.substring(0, lastIndexOf);
        String name = className.substring(lastIndexOf + 1);

        return onClass(packageName + ".Dagger" + name + "Factory")
                .call("create")
                .call("createHandler")
                .get();
    }

    public <T> IBootstrapHandler get(Class<T> clazz) {
        return get(clazz.getName());
    }
}
