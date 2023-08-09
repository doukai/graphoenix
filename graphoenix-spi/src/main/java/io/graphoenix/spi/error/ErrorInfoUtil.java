package io.graphoenix.spi.error;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import static io.graphoenix.spi.error.BaseErrorInfo.UNKNOWN_CODE;
import static io.graphoenix.spi.error.BaseErrorInfo.UNKNOWN_MESSAGE;

public enum ErrorInfoUtil {
    ERROR_CODE_UTIL;

    private static final Map<String, Integer> errorCodeMap = new HashMap<>();

    private static final Map<String, String> errorMessageMap = new HashMap<>();

    public Integer getCode(Class<? extends Throwable> type) {
        return errorCodeMap.computeIfAbsent(
                type.getCanonicalName(),
                (key) -> ServiceLoader.load(ErrorInfo.class, this.getClass().getClassLoader()).stream()
                        .map(ServiceLoader.Provider::get)
                        .flatMap(errorInfo -> errorInfo.getCode(type).stream())
                        .findFirst()
                        .orElse(UNKNOWN_CODE)
        );
    }

    public String getMessage(Class<? extends Throwable> type) {
        return errorMessageMap.computeIfAbsent(
                type.getCanonicalName(),
                (key) -> ServiceLoader.load(ErrorInfo.class, this.getClass().getClassLoader()).stream()
                        .map(ServiceLoader.Provider::get)
                        .flatMap(errorInfo -> errorInfo.getMessage(type).stream())
                        .findFirst()
                        .orElse(UNKNOWN_MESSAGE)
        );
    }
}
