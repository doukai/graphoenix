package io.graphoenix.common.pipeline.operation;

import io.graphoenix.common.pipeline.PipelineContext;
import io.graphoenix.spi.dto.type.AsyncType;
import io.graphoenix.spi.dto.type.ExecuteType;
import io.graphoenix.spi.handler.IOperationHandler;
import org.apache.commons.chain.impl.ChainBase;
import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;

public class OperationPipeline extends ChainBase {

    @Inject
    public OperationPipeline(OperationRouter router) {
        addCommand(router);
    }

    public OperationPipeline addHandler(IOperationHandler handler) {
        addCommand(new OperationHandler(handler));
        return this;
    }

    private PipelineContext fetch(String graphQL) throws Exception {
        PipelineContext pipelineContext = new PipelineContext();
        pipelineContext.addStatus(ExecuteType.SYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        pipelineContext.add(graphQL);
        this.execute(pipelineContext);
        return pipelineContext;
    }

    public <T> T fetch(String graphQL, Class<T> clazz) throws Exception {
        return fetch(graphQL).poll(clazz);
    }

    public <A, B> Pair<A, B> fetch(String graphQL, Class<A> clazzA, Class<B> clazzB) throws Exception {
        PipelineContext pipelineContext = fetch(graphQL);
        A a = pipelineContext.poll(clazzA);
        B b = pipelineContext.poll(clazzB);
        return Pair.with(a, b);
    }

    private PipelineContext fetchAsync(String graphQL) throws Exception {
        PipelineContext pipelineContext = new PipelineContext();
        pipelineContext.addStatus(ExecuteType.ASYNC);
        pipelineContext.addStatus(AsyncType.OPERATION);
        pipelineContext.add(graphQL);
        this.execute(pipelineContext);
        return pipelineContext;
    }

    public <T> T fetchAsync(String graphQL, Class<T> clazz) throws Exception {
        return fetchAsync(graphQL).poll(clazz);
    }

    public <T> Mono<T> fetchAsyncToMono(String graphQL, Class<T> clazz) throws Exception {
        return fetchAsync(graphQL).pollMono(clazz);
    }

    public <A, B> Pair<A, B> fetchAsync(String graphQL, Class<A> clazzA, Class<B> clazzB) throws Exception {
        PipelineContext pipelineContext = fetchAsync(graphQL);
        A a = pipelineContext.poll(clazzA);
        B b = pipelineContext.poll(clazzB);
        return Pair.with(a, b);
    }

    private PipelineContext fetchSelectionsAsync(String graphQL) throws Exception {
        PipelineContext pipelineContext = new PipelineContext();
        pipelineContext.addStatus(ExecuteType.ASYNC);
        pipelineContext.addStatus(AsyncType.SELECTION);
        pipelineContext.add(graphQL);
        this.execute(pipelineContext);
        return pipelineContext;
    }

    public <T> T fetchSelectionsAsync(String graphQL, Class<T> clazz) throws Exception {
        return fetchSelectionsAsync(graphQL).poll(clazz);
    }

    public <A, B> Pair<A, B> fetchSelectionsAsync(String graphQL, Class<A> clazzA, Class<B> clazzB) throws Exception {
        PipelineContext pipelineContext = fetchSelectionsAsync(graphQL);
        A a = pipelineContext.poll(clazzA);
        B b = pipelineContext.poll(clazzB);
        return Pair.with(a, b);
    }

    public <A, B> Flux<Pair<A, B>> fetchSelectionsAsyncToFlux(String graphQL, Class<A> clazzA, Class<B> clazzB) throws Exception {
        PipelineContext pipelineContext = fetchSelectionsAsync(graphQL);
        return pipelineContext.pollFluxPair(clazzA, clazzB);
    }
}
