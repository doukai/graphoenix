package io.graphoenix.spi.handler;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public interface OperationDAO {

    static <T> String fileToString(Class<T> clazz, String fileName) {
        try {
            return Files.readString(Path.of(Objects.requireNonNull(clazz.getResource(fileName)).toURI()), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    <T> T findOne(String sql, Class<T> clazz);

    <T> List<T> findAll(String sql, Class<T> clazz);

    <T> T save(String sql, Class<T> clazz);

    <T> Publisher<T> findOneAsync(String sql, Class<T> clazz);

    <T> Publisher<List<T>> findAllAsync(String sql, Class<T> clazz);

    <T> Publisher<T> saveAsync(String sql, Class<T> clazz);

    void addOperationHandlers();
}
