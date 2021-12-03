package io.graphoenix.common.pipeline;

import com.google.auto.factory.AutoFactory;
import io.graphoenix.common.pipeline.bootstrap.BootstrapPipeline;
import io.graphoenix.common.pipeline.operation.OperationPipeline;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IOperationHandler;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

@AutoFactory
public class GraphQLCodeGenerator {

    private final IGraphQLDocumentManager documentManager;
    private final List<IOperationHandler> pretreatmentHandlers;
    private final List<String> executeHandlerNames;

    public GraphQLCodeGenerator(IGraphQLDocumentManager manager,
                                List<String> bootstrapHandlerNames,
                                List<String> pretreatmentHandlerNames,
                                List<String> executeHandlerNames) throws Exception {
        BootstrapPipeline bootstrapPipeline = new BootstrapPipeline(manager);

        for (String bootstrapHandlerName : bootstrapHandlerNames) {
            bootstrapPipeline.addHandler((IBootstrapHandler) Class.forName(bootstrapHandlerName).getDeclaredConstructor().newInstance());
        }
        this.documentManager = bootstrapPipeline.buildManager();

        this.pretreatmentHandlers = new ArrayList<>();
        for (String pretreatmentHandlerName : pretreatmentHandlerNames) {
            this.pretreatmentHandlers.add((IOperationHandler) Class.forName(pretreatmentHandlerName).getDeclaredConstructor().newInstance());
        }

        this.executeHandlerNames = new ArrayList<>();
        this.executeHandlerNames.addAll(executeHandlerNames);
    }

    public Pair<String, String> pretreatment(String graphQL) throws Exception {
        return this.createOperationPipeline().fetch(graphQL, String.class, String.class);
    }

    public List<String> getExecuteHandlerNames() {
        return executeHandlerNames;
    }

    private OperationPipeline createOperationPipeline() {
        OperationPipeline operationPipeline = new OperationPipeline(this.documentManager);
        for (IOperationHandler operationHandler : pretreatmentHandlers) {
            operationPipeline.addHandler(operationHandler);
        }
        return operationPipeline;
    }
}
