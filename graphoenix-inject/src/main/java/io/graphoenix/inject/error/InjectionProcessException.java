package io.graphoenix.inject.error;

public class InjectionProcessException extends RuntimeException {

    public InjectionProcessException(InjectionProcessErrorType injectionProcessErrorType) {
        super(injectionProcessErrorType.toString());
    }
}
