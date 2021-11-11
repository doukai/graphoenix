package io.graphoenix.common.pipeline.bootstrap;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.apache.commons.chain.impl.ContextBase;

public class BootstrapContext extends ContextBase {

    private IGraphQLDocumentManager manager;
    private Object currentData;

    public IGraphQLDocumentManager getManager() {
        return manager;
    }

    public void setManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public Object getCurrentData() {
        return currentData;
    }

    public void setCurrentData(Object currentData) {
        this.currentData = currentData;
    }
}
