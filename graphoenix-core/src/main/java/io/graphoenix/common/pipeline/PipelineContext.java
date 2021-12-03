package io.graphoenix.common.pipeline;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IPipelineContext;
import org.apache.commons.chain.impl.ContextBase;

import java.util.*;

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
        return clazz.cast(dataQueue.poll());
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
