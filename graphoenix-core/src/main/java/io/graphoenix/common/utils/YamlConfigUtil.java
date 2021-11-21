package io.graphoenix.common.utils;

import one.util.streamex.StreamEx;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public enum YamlConfigUtil {

    YAML_CONFIG_UTIL;

    public <T> T loadAs(String name, Class<T> type) {
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(name);
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
