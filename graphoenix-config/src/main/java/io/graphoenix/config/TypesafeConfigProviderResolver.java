package io.graphoenix.config;

import com.google.auto.service.AutoService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;

@AutoService(ConfigProviderResolver.class)
public class TypesafeConfigProviderResolver extends ConfigProviderResolver {

    @Override
    public Config getConfig() {
        return CONFIG_UTIL.load();
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return CONFIG_UTIL.load(loader);
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
