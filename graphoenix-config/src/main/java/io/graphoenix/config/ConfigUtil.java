package io.graphoenix.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum ConfigUtil {
    CONFIG_UTil;
    private static final String RESOURCES_PATH = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);
    private static final String[] configNames = {"application.conf", "application.json", "application.properties", "reference.conf"};
    private static final Config config;

    static {
        config = Arrays.stream(Objects.requireNonNull(new File(RESOURCES_PATH).listFiles()))
                .filter(file -> Arrays.asList(configNames).contains(file.getName()))
                .findFirst()
                .map(ConfigFactory::parseFile)
                .orElseGet(ConfigFactory::load);
    }

    public Config getConfig() {
        return config;
    }

    public <T> T getValue(Class<T> propertyType) {
        ConfigProperties configProperties = propertyType.getAnnotation(ConfigProperties.class);
        return getValue(configProperties.prefix(), propertyType);
    }

    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return ConfigBeanFactory.create(config, propertyType);
    }

    public <T> Optional<T> getOptionalValue(Class<T> propertyType) {
        ConfigProperties configProperties = propertyType.getAnnotation(ConfigProperties.class);
        return getOptionalValue(configProperties.prefix(), propertyType);
    }

    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        if (config.hasPath(propertyName)) {
            return Optional.of(ConfigBeanFactory.create(config, propertyType));
        }
        return Optional.empty();
    }
}
