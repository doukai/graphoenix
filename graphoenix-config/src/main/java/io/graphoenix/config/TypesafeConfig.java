package io.graphoenix.config;

import com.typesafe.config.ConfigBeanFactory;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TypesafeConfig implements Config {

    private com.typesafe.config.Config config;

    public TypesafeConfig(com.typesafe.config.Config config) {
        this.config = config;
    }

    public com.typesafe.config.Config getConfig() {
        return config;
    }

    public TypesafeConfig setConfig(com.typesafe.config.Config config) {
        this.config = config;
        return this;
    }

    public TypesafeConfig mergeConfig(com.typesafe.config.Config config) {
        this.config = this.config.withFallback(config);
        return this;
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return ConfigBeanFactory.create(config.getConfig(propertyName), propertyType);
    }

    @Override
    public ConfigValue getConfigValue(String propertyName) {
        return new TypesafeConfigValue(config.getValue(propertyName));
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        if (config.hasPath(propertyName)) {
            return Optional.of(ConfigBeanFactory.create(config.getConfig(propertyName), propertyType));
        }
        return Optional.empty();
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return config.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return null;
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        return Optional.of(new TypesafeConverter<>(forType));
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return null;
    }
}
