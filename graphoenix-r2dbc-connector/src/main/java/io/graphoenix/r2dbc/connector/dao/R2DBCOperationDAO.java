package io.graphoenix.r2dbc.connector.dao;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.dao.BaseOperationDAO;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class R2DBCOperationDAO extends BaseOperationDAO {

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    private final QueryExecutor queryExecutor = BeanContext.get(QueryExecutor.class);

    private final MutationExecutor mutationExecutor = BeanContext.get(MutationExecutor.class);

    private final R2dbcParameterProcessor r2dbcParameterProcessor = BeanContext.get(R2dbcParameterProcessor.class);

    @Override
    public <T> T findOne(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return gsonBuilder.create().fromJson(queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).block(), beanClass);
    }

    @Override
    public <T> T findAll(String sql, Map<String, Object> parameters, Type type) {
        return gsonBuilder.create().fromJson(queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).block(), type);
    }

    @Override
    public <T> Collection<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = (new TypeToken<Collection<T>>() {
        }).getType();
        return findAll(sql, parameters, type);
    }

    @Override
    public <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return gsonBuilder.create().fromJson(mutationExecutor.executeMutations(sql, r2dbcParameterProcessor.process(parameters)).block(), beanClass);
    }

    @Override
    public <T> Mono<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).map(json -> gsonBuilder.create().fromJson(json, beanClass));
    }

    @Override
    public <T> Mono<T> findAllAsync(String sql, Map<String, Object> parameters, Type type) {
        Mono<String> jsonMono = queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters));
        return jsonMono.map(json -> gsonBuilder.create().fromJson(json, type));
    }

    @Override
    public <T> Mono<Collection<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = (new TypeToken<Collection<T>>() {
        }).getType();
        return findAllAsync(sql, parameters, type);
    }

    @Override
    public <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Mono<String> jsonMono = mutationExecutor.executeMutations(sql, r2dbcParameterProcessor.process(parameters));
        return jsonMono.map(json -> gsonBuilder.create().fromJson(json, beanClass));
    }
}
