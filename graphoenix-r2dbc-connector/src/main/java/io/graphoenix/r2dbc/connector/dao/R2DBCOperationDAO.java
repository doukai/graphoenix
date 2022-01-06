package io.graphoenix.r2dbc.connector.dao;

import com.google.gson.GsonBuilder;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.dao.BaseOperationDAO;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.ObjectCastUtil.OBJECT_CAST_UTIL;

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
