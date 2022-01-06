package io.graphoenix.spi.dao;

import io.vavr.CheckedFunction0;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BaseOperationDAO implements OperationDAO {

    protected static <T> String fileToString(Class<T> beanClass, String fileName) {
        URL fileURL = beanClass.getResource(fileName);
        assert fileURL != null;
        return CheckedFunction0.of(() -> Files.readString(Path.of(fileURL.toURI()), StandardCharsets.UTF_8)).unchecked().get();
    }
}
