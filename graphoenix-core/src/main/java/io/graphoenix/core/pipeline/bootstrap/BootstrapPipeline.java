package io.graphoenix.core.pipeline.bootstrap;

import io.graphoenix.core.pipeline.PipelineContext;
import io.graphoenix.spi.handler.IBootstrapHandler;
import org.apache.commons.chain.impl.ChainBase;

public class BootstrapPipeline extends ChainBase {

    public void execute() throws Exception {
        PipelineContext pipelineContext = new PipelineContext();
        this.execute(pipelineContext);
    }

    public BootstrapPipeline addHandler(IBootstrapHandler handler) {
        addCommand(new BootstrapHandler(handler));
        return this;
    }
}
