package io.graphoenix.common.pipeline.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.dto.OperationType;
import org.apache.commons.chain.impl.ContextBase;

public class OperationContext extends ContextBase {

    private GraphQLRequest request;
    private IGraphqlDocumentManager manager;
    private OperationType operationType;
    private Object currentData;

    public GraphQLRequest getRequest() {
        return request;
    }

    public OperationContext setRequest(GraphQLRequest request) {
        this.request = request;
        return this;
    }

    public IGraphqlDocumentManager getManager() {
        return manager;
    }

    public OperationContext setManager(IGraphqlDocumentManager manager) {
        this.manager = manager;
        return this;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public OperationContext setOperationType(OperationType operationType) {
        this.operationType = operationType;
        return this;
    }

    public Object getCurrentData() {
        return currentData;
    }

    public OperationContext setCurrentData(Object currentData) {
        this.currentData = currentData;
        return this;
    }
}
