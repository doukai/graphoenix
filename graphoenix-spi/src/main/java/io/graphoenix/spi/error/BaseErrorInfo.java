package io.graphoenix.spi.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class BaseErrorInfo implements ErrorInfo {

    private final Map<Class<? extends Throwable>, Integer> codeMap = new HashMap<>();

    private final Map<Class<? extends Throwable>, String> messageMap = new HashMap<>();

    public static final Integer UNKNOWN_CODE = -99999;

    public static final String UNKNOWN_MESSAGE = "Unknown error";

    public BaseErrorInfo() {
        register();
    }

    @Override
    public Optional<Integer> getCode(Class<? extends Throwable> type) {
        if (codeMap.containsKey(type)) {
            return Optional.of(codeMap.get(type));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getMessage(Class<? extends Throwable> type) {
        if (messageMap.containsKey(type)) {
            return Optional.of(messageMap.get(type));
        }
        return Optional.empty();
    }

    protected void put(Class<? extends Throwable> type, Integer code, String message) {
        codeMap.put(type, code);
        messageMap.put(type, message);
    }

    public abstract void register();
}
