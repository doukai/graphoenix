package io.graphoenix.common.utils;

import io.graphoenix.common.constant.Hammurabi;
import one.util.streamex.StreamEx;
import org.yaml.snakeyaml.Yaml;

import java.io.*;

public enum YamlConfigUtil {

    YAML_CONFIG_UTIL;

    public <T> T loadAs(Class<T> type) {
        String name = Hammurabi.CONFIG_FILE_NAME;
        return loadAs(name, type);
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
        Yaml yaml = new Yaml();
        return StreamEx.of(yaml.loadAll(inputStream).iterator())
                .findFirst(object -> object.getClass().isAssignableFrom(type))
                .map(type::cast)
                .orElseThrow();
    }
}
