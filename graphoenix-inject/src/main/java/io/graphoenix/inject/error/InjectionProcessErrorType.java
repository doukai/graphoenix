package io.graphoenix.inject.error;

public enum InjectionProcessErrorType {

    ROOT_PACKAGE_NOT_EXIST(-101, "can't find root package"),
    CANNOT_PARSER_SOURCE_CODE(-102, "can't parser source code: %s"),
    CANNOT_GET_COMPILATION_UNIT(-103, "can't get compilation unit of %s"),
    PUBLIC_CLASS_NOT_EXIST(-104, "public class not exist in: %s"),
    PUBLIC_ANNOTATION_NOT_EXIST(-105, "public annotation not exist in: %s"),
    CONSTRUCTOR_NOT_EXIST(-106, "can't find constructor of %s"),
    PROVIDER_TYPE_NOT_EXIST(-107, "can't find type argument of provider"),
    MODULE_PROVIDERS_METHOD_NOT_EXIST(-108, "can't find module class providers method of %s"),
    COMPONENT_GET_METHOD_NOT_EXIST(-109, "can't find component class get method of %s"),

    CONFIG_PROPERTIES_PREFIX_NOT_EXIST(-111, "prefix not exist in @ConfigProperties in: %s"),
    CONFIG_PROPERTY_NOT_EXIST(-112, "@ConfigProperty not exist on: %s"),

    UNKNOWN(-199, "unknown injection error");

    private final int code;
    private final String description;
    private Object[] variables;

    InjectionProcessErrorType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public InjectionProcessErrorType bind(Object... variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public String toString() {
        return code + ": " + String.format(description, variables);
    }
}
