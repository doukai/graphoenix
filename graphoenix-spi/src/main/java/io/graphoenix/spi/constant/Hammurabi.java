package io.graphoenix.spi.constant;

import java.io.File;

public class Hammurabi {

    public static final String JAVA_PATH = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("java").concat(File.separator);
    public static final String RESOURCES_PATH = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);
    public static final String META_INTERFACE_NAME = "Meta";
    public static final String DEPRECATED_FIELD_NAME = "isDeprecated";
    public static final String DEPRECATED_INPUT_NAME = "includeDeprecated";
    public static final String INTROSPECTION_PREFIX = "__";
}
