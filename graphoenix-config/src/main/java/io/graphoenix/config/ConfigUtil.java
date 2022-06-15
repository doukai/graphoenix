package io.graphoenix.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction3;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public enum ConfigUtil {
    CONFIG_UTIL;

    private final String[] configNames = {"application.conf", "application.json", "application.properties", "reference.conf"};
    private Config config;

    ConfigUtil() {
        this.config = ConfigFactory.load();
    }

    public ConfigUtil scan(String path) {
        this.config = Arrays.stream(Objects.requireNonNull(new File(path).listFiles()))
                .filter(file -> Arrays.asList(configNames).contains(file.getName()))
                .findFirst()
                .map(ConfigFactory::parseFile)
                .orElse(null);
        return this;
    }

    public ConfigUtil scan(Filer filer) {
        this.config = Stream.of(configNames)
                .map(configName -> CheckedFunction3.lift(filer::getResource).apply(StandardLocation.SOURCE_PATH, "", configName).getOrElse(() -> null))
                .filter(Objects::nonNull)
                .map(fileObject -> CheckedFunction0.of(fileObject.toUri()::toURL).unchecked().get())
                .map(ConfigFactory::parseURL)
                .findFirst()
                .orElse(null);
        return this;
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
        if (config != null && config.hasPath(propertyName)) {
            return Optional.of(ConfigBeanFactory.create(config.getConfig(propertyName), propertyType));
        }
        return Optional.empty();
    }
}
