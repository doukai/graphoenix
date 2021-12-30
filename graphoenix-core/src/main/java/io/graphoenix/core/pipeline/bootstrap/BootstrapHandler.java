package io.graphoenix.core.pipeline.bootstrap;

import io.graphoenix.core.pipeline.PipelineContext;
import io.graphoenix.spi.handler.IBootstrapHandler;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.core.pipeline.PipelineContext.INSTANCE_KEY;

public class BootstrapHandler implements Command {

    private final IBootstrapHandler handler;

    public BootstrapHandler(IBootstrapHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean execute(Context context) throws Exception {
        PipelineContext pipelineContext = (PipelineContext) context.get(INSTANCE_KEY);
        return this.handler.execute(pipelineContext);
    }
}
