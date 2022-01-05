package io.graphoenix.spi.dao;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface OperationDAO {

    <T> T findOne(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> List<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Publisher<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Publisher<List<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Publisher<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);
}
