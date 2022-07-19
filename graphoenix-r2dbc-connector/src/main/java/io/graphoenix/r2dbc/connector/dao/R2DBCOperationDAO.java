package io.graphoenix.r2dbc.connector.dao;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.dao.BaseOperationDAO;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import jakarta.json.bind.Jsonb;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Map;

public class R2DBCOperationDAO extends BaseOperationDAO {

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    private final R2dbcParameterProcessor r2dbcParameterProcessor;

    private final Jsonb jsonb;

    public R2DBCOperationDAO() {
        this.queryExecutor = BeanContext.get(QueryExecutor.class);
        this.mutationExecutor = BeanContext.get(MutationExecutor.class);
        this.r2dbcParameterProcessor = BeanContext.get(R2dbcParameterProcessor.class);
        this.jsonb = BeanContext.get(Jsonb.class);
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
        return toBuilder(findAsync(sql, parameters, beanClass));
    }

    @Override
    public <T> PublisherBuilder<T> findAsyncBuilder(String sql, Map<String, Object> parameters, Type type) {
        return toBuilder(findAsync(sql, parameters, type));
    }

    @Override
    public <T> PublisherBuilder<T> saveAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return toBuilder(saveAsync(sql, parameters, beanClass));
    }

    @Override
    public <T> PublisherBuilder<T> saveAsyncBuilder(String sql, Map<String, Object> parameters, Type type) {
        return toBuilder(saveAsync(sql, parameters, type));
    }
}
