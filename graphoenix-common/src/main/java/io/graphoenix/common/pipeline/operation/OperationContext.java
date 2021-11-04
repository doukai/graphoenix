package io.graphoenix.common.pipeline.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.OperationType;
import org.apache.commons.chain.impl.ContextBase;

public class OperationContext extends ContextBase {

    private IGraphqlDocumentManager manager;
    private OperationType operationType;
    private Object currentData;

    public IGraphqlDocumentManager getManager() {
        return manager;
    }

    public void setManager(IGraphqlDocumentManager manager) {
        this.manager = manager;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Object getCurrentData() {
        return currentData;
    }

    public void setCurrentData(Object currentData) {
        this.currentData = currentData;
    }
}
