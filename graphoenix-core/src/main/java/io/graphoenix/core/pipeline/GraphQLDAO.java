package io.graphoenix.core.pipeline;

import com.google.gson.GsonBuilder;
import io.graphoenix.core.pipeline.operation.OperationHandler;
import io.graphoenix.spi.dto.type.AsyncType;
import io.graphoenix.spi.dto.type.ExecuteType;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.IOperationHandler;
import org.apache.commons.chain.impl.ChainBase;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.ObjectCastUtil.OBJECT_CAST_UTIL;

public abstract class GraphQLDAO extends ChainBase {

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    protected static <T> String fileToString(Class<T> clazz, String fileName) {
        try {
            return Files.readString(Path.of(Objects.requireNonNull(clazz.getResource(fileName)).toURI()), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String query(PipelineContext pipelineContext) throws Exception {
        addOperationHandlers();
        pipelineContext.addStatus(OperationType.QUERY);
        pipelineContext.addStatus(ExecuteType.SYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        this.execute(pipelineContext);
        return pipelineContext.poll(String.class);
    }

    private Mono<String> queryAsync(PipelineContext pipelineContext) throws Exception {
        addOperationHandlers();
        pipelineContext.addStatus(OperationType.QUERY);
        pipelineContext.addStatus(ExecuteType.ASYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        this.execute(pipelineContext);
        return pipelineContext.pollMono(String.class);
    }

    private String mutation(PipelineContext pipelineContext) throws Exception {
        pipelineContext.addStatus(OperationType.MUTATION);
        pipelineContext.addStatus(ExecuteType.SYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        this.execute(pipelineContext);
        return pipelineContext.poll(String.class);
    }

    private Mono<String> mutationAsync(PipelineContext pipelineContext) throws Exception {
        addOperationHandlers();
        pipelineContext.addStatus(OperationType.MUTATION);
        pipelineContext.addStatus(ExecuteType.ASYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        this.execute(pipelineContext);
        return pipelineContext.pollMono(String.class);
    }

    protected <T> T findOne(PipelineContext pipelineContext, Class<T> clazz) throws Exception {
        String json = query(pipelineContext);
        return gsonBuilder.create().fromJson(json, clazz);
    }

    protected <T> List<T> findAll(PipelineContext pipelineContext, Class<T> clazz) throws Exception {
        String json = query(pipelineContext);
        List<?> list = gsonBuilder.create().fromJson(json, List.class);
        return list.stream()
                .map(item -> OBJECT_CAST_UTIL.cast(item, clazz))
                .collect(Collectors.toList());
    }

    protected <T> T save(PipelineContext pipelineContext, Class<T> clazz) throws Exception {
        String json = mutation(pipelineContext);
        return gsonBuilder.create().fromJson(json, clazz);
    }

    protected <T> Mono<T> findOneAsync(PipelineContext pipelineContext, Class<T> clazz) throws Exception {
        Mono<String> jsonMono = queryAsync(pipelineContext);
        return jsonMono.map(json -> gsonBuilder.create().fromJson(json, clazz));
    }

    protected <T> Mono<List<T>> findAllAsync(PipelineContext pipelineContext, Class<T> clazz) throws Exception {
        Mono<String> jsonMono = queryAsync(pipelineContext);
        return jsonMono
                .map(json -> gsonBuilder.create().fromJson(json, List.class))
                .map(list -> OBJECT_CAST_UTIL.castToList(list, clazz));
    }

    protected <T> Mono<T> saveAsync(PipelineContext pipelineContext, Class<T> clazz) throws Exception {
        Mono<String> jsonMono = mutationAsync(pipelineContext);
        return jsonMono.map(json -> gsonBuilder.create().fromJson(json, clazz));
    }

    protected void addOperationHandler(IOperationHandler handler) {
        addCommand(new OperationHandler(handler));
    }

    protected abstract void addOperationHandlers();
}
