package io.graphoenix.spi.dao;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract class BaseOperationDAO implements OperationDAO {

    protected static <T> String fileToString(Class<T> clazz, String fileName) {
        try {
            return Files.readString(Path.of(Objects.requireNonNull(clazz.getResource(fileName)).toURI()), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
}
