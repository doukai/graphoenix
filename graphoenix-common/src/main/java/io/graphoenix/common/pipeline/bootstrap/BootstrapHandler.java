package io.graphoenix.common.pipeline.bootstrap;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.bootstrap.IBootstrapHandler;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.bootstrap.BootStrapConstant.CURRENT_DATA_KEY;
import static io.graphoenix.common.pipeline.bootstrap.BootStrapConstant.MANAGER_KEY;

public class BootstrapHandler<I, O> implements Command {

    private final IBootstrapHandler<I, O> handler;

    public BootstrapHandler(IBootstrapHandler<I, O> handler) {
        this.handler = handler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Context context) throws Exception {
        IGraphqlDocumentManager iGraphqlDocumentManager = (IGraphqlDocumentManager) context.get(MANAGER_KEY);
        context.put(CURRENT_DATA_KEY, this.handler.transform(iGraphqlDocumentManager, (I) context.get(CURRENT_DATA_KEY)));
        this.handler.process(iGraphqlDocumentManager);
        return false;
    }
}
