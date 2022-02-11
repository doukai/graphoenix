package io.graphoenix.config;

import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.eclipse.microprofile.config.spi.Converter;

public class TypesafeConverter<T> implements Converter<T> {

    private final Class<T> forType;

    public TypesafeConverter(Class<T> forType) {
        this.forType = forType;
    }

    @Override
    public T convert(String value) throws IllegalArgumentException, NullPointerException {
        return ConfigBeanFactory.create(ConfigFactory.parseResources(value).resolve(), forType);
    }
}
