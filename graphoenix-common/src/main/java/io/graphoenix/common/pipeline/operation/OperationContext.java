package io.graphoenix.common.pipeline.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.type.AsyncType;
import io.graphoenix.spi.dto.type.ExecuteType;
import io.graphoenix.spi.dto.type.OperationType;
import org.apache.commons.chain.impl.ContextBase;

public class OperationContext extends ContextBase {

    private IGraphqlDocumentManager manager;
    private OperationType operationType;
    private ExecuteType executeType;
    private AsyncType asyncType;
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

    public ExecuteType getExecuteType() {
        return executeType;
    }

    public void setExecuteType(ExecuteType executeType) {
        this.executeType = executeType;
    }

    public AsyncType getAsyncType() {
        return asyncType;
    }

    public void setAsyncType(AsyncType asyncType) {
        this.asyncType = asyncType;
    }

    public Object getCurrentData() {
        return currentData;
    }

    public void setCurrentData(Object currentData) {
        this.currentData = currentData;
    }
}
