package io.graphoenix.config;

import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public enum ConfigUtil {
    CONFIG_UTIL;

    private final TypesafeConfig typesafeConfig;

    ConfigUtil() {
        typesafeConfig = new TypesafeConfig(ConfigFactory.load());
    }

    public TypesafeConfig load() {
        return typesafeConfig;
    }

    public TypesafeConfig load(ClassLoader classLoader) {
        typesafeConfig.setConfig(ConfigFactory.empty());
        return merge(classLoader);
    }

    public TypesafeConfig merge(ClassLoader classLoader) {
        return typesafeConfig.mergeConfig(ConfigFactory.load(classLoader));
    }

    public TypesafeConfig load(String path) {
        typesafeConfig.setConfig(ConfigFactory.empty());
        return merge(path);
    }

    public TypesafeConfig merge(String path) {
        try {
            Files.list(Paths.get(path))
                    .filter(filePath -> filePath.toString().endsWith(".conf") || filePath.toString().endsWith(".json") || filePath.toString().endsWith(".properties"))
                    .forEach(filePath -> typesafeConfig.mergeConfig(ConfigFactory.parseFile(filePath.toFile())));
        } catch (IOException e) {
            Logger.error(e);
        }
        return typesafeConfig;
    }

    public <T> T getConfig(Class<T> tClass) {
        try {
            return typesafeConfig.getOptionalValue(tClass.getAnnotation(ConfigProperties.class).prefix(), tClass).orElse(tClass.getDeclaredConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            Logger.error(e);
            return null;
        }
    }

    public TypesafeConfig load(Filer filer) {
        return load(getResourcesPath(filer).toString());
    }

    private Path getGeneratedSourcePath(Filer filer) {
        try {
            FileObject tmp = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", UUID.randomUUID().toString());
            Writer writer = tmp.openWriter();
            writer.write("");
            writer.close();
            Path path = Paths.get(tmp.toUri());
            Files.deleteIfExists(path);
            Path generatedSourcePath = path.getParent();
            Logger.info("generated source path: {}", generatedSourcePath.toString());
            return generatedSourcePath;
        } catch (IOException e) {
            Logger.error(e);
            return null;
        }
    }

    private Path getResourcesPath(Filer filer) {
        Path sourcePath = Objects.requireNonNull(getGeneratedSourcePath(filer)).getParent().getParent().getParent().getParent().getParent().getParent().resolve("src/main/resources");
        Logger.info("resources path: {}", sourcePath.toString());
        return sourcePath;
    }

    public <T> T getValue(Class<T> propertyType) {
        ConfigProperties configProperties = propertyType.getAnnotation(ConfigProperties.class);
        return getValue(configProperties.prefix(), propertyType);
    }

    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return ConfigBeanFactory.create(typesafeConfig.getConfig().getConfig(propertyName), propertyType);
    }

    public <T> Optional<T> getOptionalValue(Class<T> propertyType) {
        ConfigProperties configProperties = propertyType.getAnnotation(ConfigProperties.class);
        return getOptionalValue(configProperties.prefix(), propertyType);
    }

    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        if (typesafeConfig.getConfig() != null && typesafeConfig.getConfig().hasPath(propertyName)) {
            return Optional.of(ConfigBeanFactory.create(typesafeConfig.getConfig().getConfig(propertyName), propertyType));
        }
        return Optional.empty();
    }
}
