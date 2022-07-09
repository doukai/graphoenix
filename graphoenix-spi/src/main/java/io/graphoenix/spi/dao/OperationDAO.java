package io.graphoenix.spi.dao;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public interface OperationDAO {

    <T> T findOne(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> T findAll(String sql, Map<String, Object> parameters, Type type);

    <T> Collection<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Mono<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Mono<T> findAllAsync(String sql, Map<String, Object> parameters, Type type);

    <T> Mono<Collection<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> PublisherBuilder<T> findOneAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> PublisherBuilder<T> findAllAsyncBuilder(String sql, Map<String, Object> parameters, Type type);

    <T> PublisherBuilder<Collection<T>> findAllAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> PublisherBuilder<T> saveAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass);
}
