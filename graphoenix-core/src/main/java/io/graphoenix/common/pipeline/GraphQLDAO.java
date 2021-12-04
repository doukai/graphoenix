package io.graphoenix.common.pipeline;

import com.google.gson.GsonBuilder;
import io.graphoenix.common.pipeline.operation.OperationHandler;
import io.graphoenix.spi.dto.type.AsyncType;
import io.graphoenix.spi.dto.type.ExecuteType;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.IOperationHandler;
import org.apache.commons.chain.impl.ChainBase;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.graphoenix.common.utils.ObjectCastUtil.OBJECT_CAST_UTIL;

public abstract class GraphQLDAO extends ChainBase {

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    private String query(String request, Map<String, Object> parameters) throws Exception {
        addOperationHandlers();
        PipelineContext pipelineContext = new PipelineContext();
        pipelineContext.addStatus(OperationType.QUERY);
        pipelineContext.addStatus(ExecuteType.SYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        pipelineContext.add(request);
        pipelineContext.add(parameters);
        this.execute(pipelineContext);
        return pipelineContext.poll(String.class);
    }

    private Mono<String> queryAsync(String request, Map<String, Object> parameters) throws Exception {
        addOperationHandlers();
        PipelineContext pipelineContext = new PipelineContext();
        pipelineContext.addStatus(OperationType.QUERY);
        pipelineContext.addStatus(ExecuteType.ASYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        pipelineContext.add(request);
        pipelineContext.add(parameters);
        this.execute(pipelineContext);
        return pipelineContext.pollMono(String.class);
    }

    private String mutation(String request, Map<String, Object> parameters) throws Exception {
        PipelineContext pipelineContext = new PipelineContext();
        pipelineContext.addStatus(OperationType.MUTATION);
        pipelineContext.addStatus(ExecuteType.SYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        pipelineContext.add(request);
        pipelineContext.add(parameters);
        this.execute(pipelineContext);
        return pipelineContext.poll(String.class);
    }

    private Mono<String> mutationAsync(String request, Map<String, Object> parameters) throws Exception {
        addOperationHandlers();
        PipelineContext pipelineContext = new PipelineContext();
        pipelineContext.addStatus(OperationType.MUTATION);
        pipelineContext.addStatus(ExecuteType.ASYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        pipelineContext.add(request);
        pipelineContext.add(parameters);
        this.execute(pipelineContext);
        return pipelineContext.pollMono(String.class);
    }

    protected <T> T findOne(String request, Map<String, Object> parameters, Class<T> clazz) throws Exception {
        String json = query(request, parameters);
        return gsonBuilder.create().fromJson(json, clazz);
    }

    protected <T> List<T> findAll(String request, Map<String, Object> parameters, Class<T> clazz) throws Exception {
        String json = query(request, parameters);
        List<?> list = gsonBuilder.create().fromJson(json, List.class);
        return list.stream()
                .map(item -> OBJECT_CAST_UTIL.cast(item, clazz))
                .collect(Collectors.toList());
    }

    protected <T> T save(String request, Map<String, Object> parameters, Class<T> clazz) throws Exception {
        String json = mutation(request, parameters);
        return gsonBuilder.create().fromJson(json, clazz);
    }

    protected <T> Mono<T> findOneAsync(String request, Map<String, Object> parameters, Class<T> clazz) throws Exception {
        Mono<String> jsonMono = queryAsync(request, parameters);
        return jsonMono.map(json -> gsonBuilder.create().fromJson(json, clazz));
    }

    protected <T> Mono<List<T>> findAllAsync(String request, Map<String, Object> parameters, Class<T> clazz) throws Exception {
        Mono<String> jsonMono = queryAsync(request, parameters);
        return jsonMono
                .map(json -> gsonBuilder.create().fromJson(json, List.class))
                .map(list -> OBJECT_CAST_UTIL.castToList(list, clazz));
    }

    protected <T> Mono<T> saveAsync(String request, Map<String, Object> parameters, Class<T> clazz) throws Exception {
        Mono<String> jsonMono = mutationAsync(request, parameters);
        return jsonMono.map(json -> gsonBuilder.create().fromJson(json, clazz));
    }

    protected void addOperationHandler(IOperationHandler handler) {
        addCommand(new OperationHandler(handler));
    }

    protected abstract void addOperationHandlers();
}
