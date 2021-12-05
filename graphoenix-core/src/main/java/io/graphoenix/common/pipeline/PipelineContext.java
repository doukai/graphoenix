package io.graphoenix.common.pipeline;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IPipelineContext;
import org.apache.commons.chain.impl.ContextBase;
import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Stream;

import static io.graphoenix.common.utils.ObjectCastUtil.OBJECT_CAST_UTIL;

public class PipelineContext extends ContextBase implements IPipelineContext {

    public static final String INSTANCE_KEY = "instance";

    private PipelineContext instance;

    private IGraphQLDocumentManager manager;

    private final Queue<Object> dataQueue;

    private final Map<Class<?>, Enum<?>> statusMap;

    public PipelineContext() {
        this.dataQueue = new LinkedList<>();
        this.statusMap = new HashMap<>();
    }

    public PipelineContext(Object... objects) {
        this();
        this.dataQueue.addAll(Arrays.asList(objects));
    }

    @Override
    public synchronized PipelineContext getInstance() {
        if (instance == null) {
            instance = this;
        }
        return instance;
    }

    @Override
    public IGraphQLDocumentManager getManager() {
        return manager;
    }

    @Override
    public void setManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    @Override
    public void addStatus(Enum<?> status) {
        this.statusMap.put(status.getClass(), status);
    }

    @Override
    public <T> T getStatus(Class<T> clazz) throws ClassCastException {
        return clazz.cast(this.statusMap.get(clazz));
    }

    @Override
    public PipelineContext add(Object object) {
        dataQueue.add(object);
        return this;
    }

    @Override
    public <T> T poll(Class<T> clazz) throws ClassCastException {
        return OBJECT_CAST_UTIL.cast(dataQueue.poll(), clazz);
    }

    @Override
    public <K, V> Map<K, V> pollMap(Class<K> keyClazz, Class<V> valueClazz) throws ClassCastException {
        return OBJECT_CAST_UTIL.castToMap(dataQueue.poll(), keyClazz, valueClazz);
    }

    @Override
    public <T> Stream<T> pollStream(Class<T> clazz) throws ClassCastException {
        return OBJECT_CAST_UTIL.castToStream(dataQueue.poll(), clazz);
    }

    @Override
    public <T> Mono<T> pollMono(Class<T> clazz) throws ClassCastException {
        return OBJECT_CAST_UTIL.castToMono(dataQueue.poll(), clazz);
    }

    @Override
    public <T> Flux<T> pollFlux(Class<T> clazz) throws ClassCastException {
        return OBJECT_CAST_UTIL.castToFlux(dataQueue.poll(), clazz);
    }

    @Override
    public <K, V> Stream<Pair<K, V>> pollStreamPair(Class<K> clazz0, Class<V> clazz1) throws ClassCastException {
        return OBJECT_CAST_UTIL.castToStreamPair(dataQueue.poll(), clazz0, clazz1);
    }

    @Override
    public <K, V> Mono<Pair<K, V>> pollMonoPair(Class<K> clazz0, Class<V> clazz1) throws ClassCastException {
        return OBJECT_CAST_UTIL.castToMonoPair(dataQueue.poll(), clazz0, clazz1);
    }

    @Override
    public <K, V> Flux<Pair<K, V>> pollFluxPair(Class<K> clazz0, Class<V> clazz1) throws ClassCastException {
        return OBJECT_CAST_UTIL.castToFluxPair(dataQueue.poll(), clazz0, clazz1);
    }

    @Override
    public Object poll() {
        return dataQueue.poll();
    }


    @Override
    public <T> T element(Class<T> clazz) throws ClassCastException {
        return clazz.cast(dataQueue.element());
    }

    @Override
    public Object element() {
        return dataQueue.element();
    }
}
