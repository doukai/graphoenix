package io.graphoenix.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public enum ConfigUtil {
    CONFIG_UTIL;

    private Config config;
    private final TypesafeConfig typesafeConfig;

    ConfigUtil() {
        config = ConfigFactory.load();
        typesafeConfig = new TypesafeConfig(config);
    }

    public TypesafeConfig load() {
        return typesafeConfig;
    }

    public TypesafeConfig load(ClassLoader classLoader) {
        config = ConfigFactory.load(classLoader);
        return typesafeConfig;
    }

    public TypesafeConfig load(String path) {
        try {
            File file = new File(path);
            URL url = file.toURI().toURL();
            URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<URLClassLoader> urlClass = URLClassLoader.class;
            Method method = urlClass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(urlClassLoader, url);
            config = ConfigFactory.load(urlClassLoader);
        } catch (NoSuchMethodException | MalformedURLException | InvocationTargetException | IllegalAccessException e) {
            Logger.error(e);
        }
        return typesafeConfig;
    }

    public TypesafeConfig load(Filer filer) {
        config = ConfigFactory.load(Objects.requireNonNull(getSourcePath(filer)).toString());
        return typesafeConfig;
    }

    private Path getSourcePath(Filer filer) {
        try {
            FileObject tmp = filer.createResource(StandardLocation.SOURCE_PATH, "", UUID.randomUUID().toString());
            Writer writer = tmp.openWriter();
            writer.write("");
            writer.close();
            Path path = Paths.get(tmp.toUri());
            Files.deleteIfExists(path);
            Path generatedSourcePath = path.getParent();
            Logger.info("source path: {}", generatedSourcePath.toString());
            return generatedSourcePath;
        } catch (IOException e) {
            Logger.error(e);
            return null;
        }
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
