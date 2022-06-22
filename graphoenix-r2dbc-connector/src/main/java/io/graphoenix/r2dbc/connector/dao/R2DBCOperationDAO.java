package io.graphoenix.r2dbc.connector.dao;

import com.google.gson.reflect.TypeToken;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.dao.BaseOperationDAO;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
        try {
            return findOneAsync(sql, parameters, beanClass).toFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(e);
            throw new GraphQLErrors(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T findAll(String sql, Map<String, Object> parameters, Type type) {
        try {
            return (T) findAllAsync(sql, parameters, type).toFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(e);
            throw new GraphQLErrors(e);
        }
    }

    @Override
    public <T> Collection<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = (new TypeToken<Collection<T>>() {
        }).getType();
        return findAll(sql, parameters, type);
    }

    @Override
    public <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        try {
            return saveAsync(sql, parameters, beanClass).toFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(e);
            throw new GraphQLErrors(e);
        }
    }

    @Override
    public <T> Mono<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters))
                .map(jsonString -> jsonToType(jsonString, beanClass));
    }

    @Override
    public <T> Mono<T> findAllAsync(String sql, Map<String, Object> parameters, Type type) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters))
                .map(jsonString -> jsonToType(jsonString, type));
    }

    @Override
    public <T> Mono<Collection<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = (new TypeToken<Collection<T>>() {
        }).getType();
        return findAllAsync(sql, parameters, type);
    }

    @Override
    public <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return mutationExecutor.executeMutations(sql, r2dbcParameterProcessor.process(parameters))
                .map(jsonString -> jsonToType(jsonString, beanClass));
    }
}
