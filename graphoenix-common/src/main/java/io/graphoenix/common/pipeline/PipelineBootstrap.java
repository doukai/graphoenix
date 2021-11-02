package io.graphoenix.common.pipeline;

import io.graphoenix.common.pipeline.bootstrap.BootstrapPipeline;
import io.graphoenix.common.pipeline.operation.OperationPipeline;

public class PipelineBootstrap {

    OperationPipeline create() throws Exception {

        BootstrapPipeline bootstrapPipeline = new BootstrapPipeline();

        OperationPipeline operationPipeline = new OperationPipeline(bootstrapPipeline.buildManager());

        return operationPipeline;
    }
}
