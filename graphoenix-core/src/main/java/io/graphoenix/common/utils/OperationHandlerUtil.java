package io.graphoenix.common.utils;

import io.graphoenix.spi.handler.IOperationHandler;

import static org.joor.Reflect.onClass;

public enum OperationHandlerUtil {

    OPERATION_HANDLER_UTIL;

    public IOperationHandler get(String className) {
        int lastIndexOf = className.lastIndexOf(".");
        String packageName = className.substring(0, lastIndexOf);
        String name = className.substring(lastIndexOf + 1);

        return onClass(packageName + ".Dagger" + name + "Factory")
                .call("create")
                .call("createHandler")
                .get();
    }

    public <T> IOperationHandler get(Class<T> clazz) {
        return get(clazz.getName());
    }
}
