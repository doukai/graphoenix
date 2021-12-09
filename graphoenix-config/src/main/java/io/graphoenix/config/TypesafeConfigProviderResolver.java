package io.graphoenix.config;

import com.google.auto.service.AutoService;
import com.typesafe.config.ConfigFactory;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

@AutoService(TypesafeConfigProviderResolver.class)
public class TypesafeConfigProviderResolver extends ConfigProviderResolver {

    @Override
    public Config getConfig() {
        return new TypesafeConfig(ConfigFactory.load());
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return new TypesafeConfig(ConfigFactory.load(loader));
    }

    @Override
    public ConfigBuilder getBuilder() {
        return null;
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
    }

    @Override
    public void releaseConfig(Config config) {
    }
}
