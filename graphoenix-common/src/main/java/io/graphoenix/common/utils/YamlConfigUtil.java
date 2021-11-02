package io.graphoenix.common.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public enum YamlConfigUtil {

    YAML_CONFIG_UTIL;

    public <T> T loadAs(String name, Class<T> type) {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(name);
        return yaml.loadAs(inputStream, type);
    }
}
