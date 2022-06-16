package io.graphoenix.r2dbc.connector.dao;

import io.graphoenix.r2dbc.connector.config.R2DBCConfig;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.r2dbc.connector.connection.ConnectionFactoryCreator;
import io.graphoenix.r2dbc.connector.connection.ConnectionPoolCreator;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.spi.dao.BaseOperationDAO;
import jakarta.enterprise.context.ApplicationScoped;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

@ApplicationScoped
public class R2DBCOperationDAOBuilder extends BaseOperationDAO {

    private R2DBCOperationDAO r2DBCOperationDAO;

    public void build(R2DBCConfig r2DBCConfig) {
        ConnectionFactoryCreator connectionFactoryCreator = new ConnectionFactoryCreator(r2DBCConfig);
        ConnectionPoolCreator connectionPoolCreator = new ConnectionPoolCreator(connectionFactoryCreator, r2DBCConfig);
        ConnectionCreator connectionCreator = new ConnectionCreator(connectionFactoryCreator, connectionPoolCreator, r2DBCConfig);
        MutationExecutor mutationExecutor = new MutationExecutor(connectionCreator);
        QueryExecutor queryExecutor = new QueryExecutor(connectionCreator);
        this.r2DBCOperationDAO = new R2DBCOperationDAO(queryExecutor, mutationExecutor);
    }

    @Override
    public <T> T findOne(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return r2DBCOperationDAO.findOne(sql, parameters, beanClass);
    }

    @Override
    public <T> T findAll(String sql, Map<String, Object> parameters, Type type) {
        return r2DBCOperationDAO.findAll(sql, parameters, type);
    }

    @Override
    public <T> Collection<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return r2DBCOperationDAO.findAll(sql, parameters, beanClass);
    }

    @Override
    public <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return r2DBCOperationDAO.save(sql, parameters, beanClass);
    }

    @Override
    public <T> Mono<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return r2DBCOperationDAO.findOneAsync(sql, parameters, beanClass);
    }

    @Override
    public <T> Mono<T> findAllAsync(String sql, Map<String, Object> parameters, Type type) {
        return r2DBCOperationDAO.findAllAsync(sql, parameters, type);
    }

    @Override
    public <T> Mono<Collection<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return r2DBCOperationDAO.findAllAsync(sql, parameters, beanClass);
    }

    @Override
    public <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return r2DBCOperationDAO.saveAsync(sql, parameters, beanClass);
    }
}
