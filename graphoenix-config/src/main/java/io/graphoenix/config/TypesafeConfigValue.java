package io.graphoenix.config;

import org.eclipse.microprofile.config.ConfigValue;

public class TypesafeConfigValue implements ConfigValue {

    private final com.typesafe.config.ConfigValue configValue;

    public TypesafeConfigValue(com.typesafe.config.ConfigValue configValue) {
        this.configValue = configValue;
    }

    @Override
    public String getName() {
        return configValue.valueType().name();
    }

    @Override
    public String getValue() {
        return configValue.render();
    }

    @Override
    public String getRawValue() {
        return configValue.origin().resource();
    }

    @Override
    public String getSourceName() {
        return configValue.origin().filename();
    }

    @Override
    public int getSourceOrdinal() {
        return configValue.origin().lineNumber();
    }
}
