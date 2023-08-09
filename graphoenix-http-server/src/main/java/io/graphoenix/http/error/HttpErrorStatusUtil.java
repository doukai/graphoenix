package io.graphoenix.http.error;

import io.graphoenix.spi.error.HttpErrorStatus;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public enum HttpErrorStatusUtil {
    HTTP_ERROR_STATUS_UTIL;

    private static final Map<String, HttpResponseStatus> errorStatusMap = new HashMap<>();

    public HttpResponseStatus getStatus(Class<? extends Throwable> type) {
        return errorStatusMap.computeIfAbsent(
                type.getCanonicalName(),
                (key) -> ServiceLoader.load(HttpErrorStatus.class, this.getClass().getClassLoader()).stream()
                        .map(ServiceLoader.Provider::get)
                        .flatMap(httpErrorStatus -> httpErrorStatus.getStatus(type).stream())
                        .findFirst()
                        .map(HttpResponseStatus::valueOf)
                        .orElse(HttpResponseStatus.BAD_REQUEST)
        );
    }
}
