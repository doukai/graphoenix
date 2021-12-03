package io.graphoenix.common.pipeline;

import com.google.auto.factory.AutoFactory;
import com.google.gson.Gson;
import io.graphoenix.common.pipeline.operation.OperationPipeline;
import io.graphoenix.spi.handler.IOperationHandler;
import org.apache.commons.chain.impl.ChainBase;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static io.graphoenix.common.utils.ObjectCastUtil.OBJECT_CAST_UTIL;

@AutoFactory
public class GraphQLDAO extends ChainBase {

    private final IOperationHandler[] operationHandlers;

    public GraphQLDAO(IOperationHandler[] operationHandlers) throws Exception {
        this.operationHandlers = operationHandlers;
    }

    public <T> T findOne(String request, Class<T> clazz) throws Exception {
        String json = this.createOperationPipeline().fetch(request, String.class);
        return new Gson().fromJson(json, clazz);
    }

    public <T> List<T> findAll(String request, Class<T> clazz) throws Exception {
        String json = this.createOperationPipeline().fetch(request, String.class);
        List<?> list = new Gson().fromJson(json, List.class);
        return list.stream()
                .map(item -> OBJECT_CAST_UTIL.cast(item, clazz))
                .collect(Collectors.toList());
    }

    public <T> T save(String request, Class<T> clazz) throws Exception {
        String json = this.createOperationPipeline().fetch(request, String.class);
        return new Gson().fromJson(json, clazz);
    }

    public <T> Mono<T> findOneAsync(String request, Class<T> clazz) throws Exception {
        Mono<String> stringMono = this.createOperationPipeline().fetchAsyncToMono(request, String.class);
        return stringMono.map(json -> new Gson().fromJson(json, clazz));
    }

    public <T> Mono<List<T>> findAllAsync(String request, Class<T> clazz) throws Exception {
        Mono<String> stringMono = this.createOperationPipeline().fetchAsyncToMono(request, String.class);
        return stringMono
                .map(json -> new Gson().fromJson(json, List.class))
                .map(list -> OBJECT_CAST_UTIL.castToList(list, clazz));
    }

    public <T> Mono<T> saveAsync(String request, Class<T> clazz) throws Exception {
        Mono<String> stringMono = this.createOperationPipeline().fetchAsyncToMono(request, String.class);
        return stringMono.map(json -> new Gson().fromJson(json, clazz));
    }

    private OperationPipeline createOperationPipeline() {
        return null;
    }
}
