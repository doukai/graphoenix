package io.graphoenix.common.pipeline.bootstrap;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.bootstrap.BootstrapConstant.CURRENT_DATA_KEY;
import static io.graphoenix.common.pipeline.bootstrap.BootstrapConstant.MANAGER_KEY;

public class BootstrapHandler implements Command {

    private final IBootstrapHandler handler;

    public BootstrapHandler(IBootstrapHandler handler) {
        this.handler = handler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Context context) throws Exception {
        IGraphqlDocumentManager iGraphqlDocumentManager = (IGraphqlDocumentManager) context.get(MANAGER_KEY);
        context.put(CURRENT_DATA_KEY, this.handler.transform(iGraphqlDocumentManager, context.get(CURRENT_DATA_KEY)));
        return false;
    }
}
