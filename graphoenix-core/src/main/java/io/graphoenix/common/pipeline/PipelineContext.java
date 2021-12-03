package io.graphoenix.common.pipeline;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IPipelineContext;
import org.apache.commons.chain.impl.ContextBase;

import java.util.Queue;

public class PipelineContext extends ContextBase implements IPipelineContext {

    public static final String INSTANCE_KEY = "instance";

    private PipelineContext instance;

    private IGraphQLDocumentManager manager;

    private Queue<Object> dataQueue;

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
}
