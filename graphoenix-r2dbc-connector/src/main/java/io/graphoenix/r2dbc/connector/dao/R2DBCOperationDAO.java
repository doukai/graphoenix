package io.graphoenix.r2dbc.connector.dao;

import com.google.common.reflect.TypeToken;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.core.dao.BaseOperationDAO;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class R2DBCOperationDAO extends BaseOperationDAO {

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    private final R2dbcParameterProcessor r2dbcParameterProcessor;

    public R2DBCOperationDAO() {
        this.queryExecutor = BeanContext.get(QueryExecutor.class);
        this.mutationExecutor = BeanContext.get(MutationExecutor.class);
        this.r2dbcParameterProcessor = BeanContext.get(R2dbcParameterProcessor.class);
    }

    @Override
    public <T> T findOne(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return findOneAsync(sql, parameters, beanClass).block();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T findAll(String sql, Map<String, Object> parameters, Type type) {
        return (T) findAllAsync(sql, parameters, type).block();
    }

    @Override
    public <T> Collection<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = new TypeToken<Collection<T>>() {
        }.getType();
        return findAll(sql, parameters, type);
    }

    @Override
    public <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return saveAsync(sql, parameters, beanClass).block();
    }

    @Override
    public <T> Mono<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters))
                .mapNotNull(jsonString -> jsonToType(jsonString, beanClass));
    }

    @Override
    public <T> Mono<T> findAllAsync(String sql, Map<String, Object> parameters, Type type) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters))
                .mapNotNull(jsonString -> jsonToType(jsonString, type));
    }

    @Override
    public <T> Mono<Collection<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = new TypeToken<Collection<T>>() {
        }.getType();
        return findAllAsync(sql, parameters, type);
    }

    @Override
    public <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return mutationExecutor.executeMutations(sql, r2dbcParameterProcessor.process(parameters))
                .mapNotNull(jsonString -> jsonToType(jsonString, beanClass));
    }

    @Override
    public <T> PublisherBuilder<T> findOneAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return toBuilder(findOneAsync(sql, parameters, beanClass));
    }

    @Override
    public <T> PublisherBuilder<T> findAllAsyncBuilder(String sql, Map<String, Object> parameters, Type type) {
        return toBuilder(findAllAsync(sql, parameters, type));
    }

    @Override
    public <T> PublisherBuilder<Collection<T>> findAllAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = new TypeToken<Collection<T>>() {
        }.getType();
        return findAllAsyncBuilder(sql, parameters, type);
    }

    @Override
    public <T> PublisherBuilder<T> saveAsyncBuilder(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return toBuilder(saveAsync(sql, parameters, beanClass));
    }
}
