package io.graphoenix.common.pipeline.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.common.pipeline.PipelineContext;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.PipelineContext.INSTANCE_KEY;

public class OperationRouter implements Command {

    @Override
    public boolean execute(Context context) {

        PipelineContext pipelineContext = (PipelineContext) context.get(INSTANCE_KEY);
        String graphQL = pipelineContext.element(String.class);
        IGraphQLDocumentManager manager = pipelineContext.getManager();
        GraphqlParser.OperationTypeContext operationTypeContext = manager.getOperationType(graphQL);
        if (operationTypeContext == null || operationTypeContext.QUERY() != null) {
            pipelineContext.addStatus(OperationType.QUERY);
        } else if (operationTypeContext.MUTATION() != null) {
            pipelineContext.addStatus(OperationType.MUTATION);
        } else if (operationTypeContext.SUBSCRIPTION() != null) {
            pipelineContext.addStatus(OperationType.SUBSCRIPTION);
        }
        return false;
    }
}
