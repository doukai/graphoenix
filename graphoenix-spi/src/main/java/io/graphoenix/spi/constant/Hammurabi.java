package io.graphoenix.spi.constant;

import java.io.File;
import java.util.Objects;

import static com.google.common.base.StandardSystemProperty.USER_DIR;

public class Hammurabi {

    public static final String JAVA_PATH = Objects.requireNonNull(USER_DIR.value()).concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("java").concat(File.separator);
    public static final String RESOURCES_PATH = Objects.requireNonNull(USER_DIR.value()).concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);
    public static final String META_INTERFACE_NAME = "Meta";
    public static final String DEPRECATED_FIELD_NAME = "isDeprecated";
    public static final String DEPRECATED_INPUT_NAME = "includeDeprecated";
    public static final String INTROSPECTION_PREFIX = "__";
    public static final String[] INVOKE_DIRECTIVES = {"invoke"};
}
