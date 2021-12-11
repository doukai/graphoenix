package io.graphoenix.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.eclipse.microprofile.config.ConfigValue;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum CompileConfig {
    COMPILE_CONFIG;
    private static final String[] configNames = {"application.conf", "application.json", "application.properties", "reference.conf"};
    private static final String resourcesPath = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);
    private static final Config config;

    static {
        config = Arrays.stream(Objects.requireNonNull(new File(resourcesPath).listFiles()))
                .filter(file -> Arrays.asList(configNames).contains(file.getName()))
                .map(ConfigFactory::parseFile)
                .findFirst()
                .orElseThrow();
    }

    public Config getConfig() {
        return config;
    }

    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return ConfigBeanFactory.create(config, propertyType);
    }

    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        if (config.hasPath(propertyName)) {
            return Optional.of(ConfigBeanFactory.create(config, propertyType));
        }
        return Optional.empty();
    }
}
