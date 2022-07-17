package io.graphoenix.core.produces;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

@ApplicationScoped
public class ConfigProducer {

    @Produces
    @ApplicationScoped
    public ConfigProviderResolver configProviderResolver() {
        return ConfigProviderResolver.instance();
    }

    @Produces
    @ApplicationScoped
    public Config config() {
        return configProviderResolver().getConfig();
    }
}
