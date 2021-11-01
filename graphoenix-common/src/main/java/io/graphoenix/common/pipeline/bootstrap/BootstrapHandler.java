package io.graphoenix.common.pipeline.bootstrap;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.bootstrap.BootStrapConstant.MANAGER_KEY;

public abstract class BootstrapHandler implements Command {

    @Override
    public boolean execute(Context context) throws Exception {
        IGraphqlDocumentManager iGraphqlDocumentManager = (IGraphqlDocumentManager) context.get(MANAGER_KEY);
        process(iGraphqlDocumentManager);
        return false;
    }

    abstract void process(IGraphqlDocumentManager manager) throws Exception;
}
