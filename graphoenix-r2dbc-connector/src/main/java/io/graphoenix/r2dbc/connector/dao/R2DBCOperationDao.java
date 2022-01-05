package io.graphoenix.r2dbc.connector.dao;

import com.google.gson.GsonBuilder;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.dao.OperationDAO;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.ObjectCastUtil.OBJECT_CAST_UTIL;

public abstract class R2DBCOperationDao implements OperationDAO {

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    private final R2dbcParameterProcessor r2dbcParameterProcessor;

    public R2DBCOperationDao(QueryExecutor queryExecutor, MutationExecutor mutationExecutor, R2dbcParameterProcessor r2dbcParameterProcessor) {
        this.queryExecutor = queryExecutor;
        this.mutationExecutor = mutationExecutor;
        this.r2dbcParameterProcessor = r2dbcParameterProcessor;
    }

    @Override
    public <T> T findOne(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return gsonBuilder.create().fromJson(queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).block(), beanClass);
    }

    @Override
    public <T> List<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        List<?> list = gsonBuilder.create().fromJson(queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).block(), List.class);
        return list.stream()
                .map(item -> OBJECT_CAST_UTIL.cast(item, beanClass))
                .collect(Collectors.toList());
    }

    @Override
    public <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return gsonBuilder.create().fromJson(mutationExecutor.executeMutations(Stream.of(sql.split(";")), r2dbcParameterProcessor.process(parameters)).block(), beanClass);
    }

    @Override
    public <T> Mono<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).map(json -> gsonBuilder.create().fromJson(json, beanClass));
    }

    @Override
    public <T> Mono<List<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Mono<String> jsonMono = queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters));
        return jsonMono
                .map(json -> gsonBuilder.create().fromJson(json, List.class))
                .map(list -> OBJECT_CAST_UTIL.castToList(list, beanClass));
    }

    @Override
    public <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Mono<String> jsonMono = mutationExecutor.executeMutations(Stream.of(sql.split(";")), r2dbcParameterProcessor.process(parameters));
        return jsonMono.map(json -> gsonBuilder.create().fromJson(json, beanClass));
    }
}
