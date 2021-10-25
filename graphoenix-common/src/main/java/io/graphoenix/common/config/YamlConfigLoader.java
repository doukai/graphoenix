package io.graphoenix.common.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public enum YamlConfigLoader {

    YAML_CONFIG_LOADER;

    public <T> T loadAs(String name, Class<T> type) {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(name);
        return yaml.loadAs(inputStream, type);
    }
}
