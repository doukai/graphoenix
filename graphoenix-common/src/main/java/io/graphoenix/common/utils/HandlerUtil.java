package io.graphoenix.common.utils;

import java.util.ServiceLoader;

public enum HandlerUtil {

    HANDLER_UTIL;

    public <T> T create(Class<T> handlerClass) {
        ServiceLoader<T> loader = ServiceLoader.load(handlerClass);
        return loader.findFirst().orElse(null);
    }
}
