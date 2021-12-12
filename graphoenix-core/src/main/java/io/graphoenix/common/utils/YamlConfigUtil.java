package io.graphoenix.common.utils;

import io.graphoenix.spi.constant.Hammurabi;

import java.io.*;

public enum YamlConfigUtil {

    YAML_CONFIG_UTIL;

    public <T> T loadAs(Class<T> type) {
        return loadAs("name", type);
    }

    public <T> T loadAs(String name, Class<T> type) {

        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(name);
        if (inputStream == null) {
            String resourcesPath = System.getProperty("user.dir")
                    .concat(File.separator)
                    .concat("src")
                    .concat(File.separator)
                    .concat("main")
                    .concat(File.separator)
                    .concat("resources")
                    .concat(File.separator)
                    .concat(name);
            try {
                inputStream = new FileInputStream(resourcesPath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return loadAs(inputStream, type);
    }

    public <T> T loadAs(InputStream inputStream, Class<T> type) {
        return null;
    }
}
