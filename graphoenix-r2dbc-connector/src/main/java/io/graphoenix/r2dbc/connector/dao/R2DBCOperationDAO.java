package io.graphoenix.r2dbc.connector.dao;

import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.dao.OperationDAO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.bind.Jsonb;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Map;

@ApplicationScoped
@Named("r2dbc")
public class R2DBCOperationDAO implements OperationDAO {

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    private final R2dbcParameterProcessor r2dbcParameterProcessor;

    private final Jsonb jsonb;

    private final ReactiveStreamsFactory reactiveStreamsFactory;

    @Inject
    public R2DBCOperationDAO(QueryExecutor queryExecutor, MutationExecutor mutationExecutor, R2dbcParameterProcessor r2dbcParameterProcessor, Jsonb jsonb, ReactiveStreamsFactory reactiveStreamsFactory) {
        this.queryExecutor = queryExecutor;
        this.mutationExecutor = mutationExecutor;
        this.r2dbcParameterProcessor = r2dbcParameterProcessor;
        this.jsonb = jsonb;
        this.reactiveStreamsFactory = reactiveStreamsFactory;
    }

    @Override
    public <T> T find(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return findAsync(sql, parameters, beanClass).block();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T find(String sql, Map<String, Object> parameters, Type type) {
        return (T) findAsync(sql, parameters, type).block();
    }

    @Override
    public <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return saveAsync(sql, parameters, beanClass).block();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T save(String sql, Map<String, Object> parameters, Type type) {
        return (T) saveAsync(sql, parameters, type).block();
    }

    @Override
    public <T> Mono<T> findAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters))
                .mapNotNull(jsonString -> jsonb.fromJson(jsonString, beanClass));
    }

    @Override
    public <T> Mono<T> findAsync(String sql, Map<String, Object> parameters, Type type) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters))
                .mapNotNull(jsonString -> jsonb.fromJson(jsonString, type));
    }

    @Override
    public <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return mutationExecutor.executeMutations(sql, r2dbcParameterProcessor.process(parameters))
                .mapNotNull(jsonString -> jsonb.fromJson(jsonString, beanClass));
    }

    @Override
    public <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Type type) {
        return mutationExecutor.executeMutations(sql, r2dbcParameterProcessor.process(parameters))
                .mapNotNull(jsonString -> jsonb.fromJson(jsonString, type));
    }

    @Override
    public <T> PublisherBuilder<T> findAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return reactiveStreamsFactory.fromPublisher(findAsync(sql, parameters, beanClass));
    }

    @Override
    public <T> PublisherBuilder<T> findAsyncBuilder(String sql, Map<String, Object> parameters, Type type) {
        return reactiveStreamsFactory.fromPublisher(findAsync(sql, parameters, type));
    }

    @Override
    public <T> PublisherBuilder<T> saveAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return reactiveStreamsFactory.fromPublisher(saveAsync(sql, parameters, beanClass));
    }

    @Override
    public <T> PublisherBuilder<T> saveAsyncBuilder(String sql, Map<String, Object> parameters, Type type) {
        return reactiveStreamsFactory.fromPublisher(saveAsync(sql, parameters, type));
    }
}
