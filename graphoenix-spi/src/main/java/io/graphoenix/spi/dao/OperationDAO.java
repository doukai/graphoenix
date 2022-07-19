package io.graphoenix.spi.dao;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Map;

public interface OperationDAO {

    <T> T find(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> T find(String sql, Map<String, Object> parameters, Type type);

    <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> T save(String sql, Map<String, Object> parameters, Type type);

    <T> Mono<T> findAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Mono<T> findAsync(String sql, Map<String, Object> parameters, Type type);

    <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Type type);

    <T> PublisherBuilder<T> findAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> PublisherBuilder<T> findAsyncBuilder(String sql, Map<String, Object> parameters, Type type);

    <T> PublisherBuilder<T> saveAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass);

    <T> PublisherBuilder<T> saveAsyncBuilder(String sql, Map<String, Object> parameters, Type type);
}
