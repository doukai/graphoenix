package io.graphoenix.core.error;

public class ElementProcessException extends RuntimeException {

    public ElementProcessException(ElementProcessErrorType elementProcessErrorType) {
        super(elementProcessErrorType.toString());
    }
}
