package io.graphoenix.inject.error;

public enum InjectionErrorType {

    ROOT_PACKAGE_NOT_EXIST(-201, "can't find root package"),
    CANNOT_PARSER_SOURCE_CODE(-201, "can't parser source code: %s"),
    CANNOT_GET_PROXY_COMPILATION_UNIT(-201, "can't get proxy compilation unit of %s"),
    CONSTRUCTOR_NOT_EXIST(-201, "can't find constructor of %s"),
    MODULE_PROVIDERS_METHOD_NOT_EXIST(-201, "can't find module class providers method of %s"),
    COMPONENT_GET_METHOD_NOT_EXIST(-201, "can't find component class get method of %s"),
    PUBLIC_CLASS_NOT_EXIST(-101, "public class not exist in: %s"),
    PROVIDER_TYPE_NOT_EXIST(-101, "can't find type argument of provider"),
    PUBLIC_ANNOTATION_NOT_EXIST(-101, "public annotation not exist in: %s"),

    UNKNOWN(-199, "unknown error");

    private final int code;
    private final String description;
    private Object[] variables;

    InjectionErrorType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public InjectionErrorType bind(Object... variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public String toString() {
        return code + ": " + String.format(description, variables);
    }
}
