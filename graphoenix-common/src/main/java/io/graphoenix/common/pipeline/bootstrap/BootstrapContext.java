package io.graphoenix.common.pipeline.bootstrap;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import org.apache.commons.chain.impl.ContextBase;

public class BootstrapContext extends ContextBase {

    private IGraphqlDocumentManager manager;
    private Object currentData;

    public IGraphqlDocumentManager getManager() {
        return manager;
    }

    public void setManager(IGraphqlDocumentManager manager) {
        this.manager = manager;
    }

    public Object getCurrentData() {
        return currentData;
    }

    public void setCurrentData(Object currentData) {
        this.currentData = currentData;
    }
}
