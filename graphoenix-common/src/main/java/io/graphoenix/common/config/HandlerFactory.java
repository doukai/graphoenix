package io.graphoenix.common.config;

import java.util.ServiceLoader;

public enum HandlerFactory {

    HANDLER_FACTORY;

    public <T> T create(Class<T> handlerClass) {
        ServiceLoader<T> loader = ServiceLoader.load(handlerClass);
        return loader.findFirst().orElse(null);
    }
}
