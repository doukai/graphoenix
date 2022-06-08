package io.graphoenix.spi.dao;

import org.reactivestreams.Publisher;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public interface OperationDAO {

    <T> T findOne(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> T findAll(String sql, Map<String, Object> parameters, Type type);

    <T> Collection<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Publisher<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Publisher<T> findAllAsync(String sql, Map<String, Object> parameters, Type type);

    <T> Publisher<Collection<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Publisher<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);
}
