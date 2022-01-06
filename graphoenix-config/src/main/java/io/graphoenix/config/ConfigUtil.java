package io.graphoenix.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static io.graphoenix.spi.constant.Hammurabi.RESOURCES_PATH;

public enum ConfigUtil {
    CONFIG_UTIL(),
    RESOURCES_CONFIG_UTIL(RESOURCES_PATH);
    private final String[] configNames = {"application.conf", "application.json", "application.properties", "reference.conf"};
    private final Config config;

    ConfigUtil() {
        this.config = ConfigFactory.load();
    }

    ConfigUtil(String path) {
        this.config = Arrays.stream(Objects.requireNonNull(new File(path).listFiles()))
                .filter(file -> Arrays.asList(configNames).contains(file.getName()))
                .findFirst()
                .map(ConfigFactory::parseFile)
                .orElseThrow();
    }

    public <T> T getValue(Class<T> propertyType) {
        ConfigProperties configProperties = propertyType.getAnnotation(ConfigProperties.class);
        return getValue(configProperties.prefix(), propertyType);
    }

    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return ConfigBeanFactory.create(config.getConfig(propertyName), propertyType);
    }

    public <T> Optional<T> getOptionalValue(Class<T> propertyType) {
        ConfigProperties configProperties = propertyType.getAnnotation(ConfigProperties.class);
        return getOptionalValue(configProperties.prefix(), propertyType);
    }

    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        if (config.hasPath(propertyName)) {
            return Optional.of(ConfigBeanFactory.create(config.getConfig(propertyName), propertyType));
        }
        return Optional.empty();
    }
}
