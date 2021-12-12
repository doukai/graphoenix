package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import javax.inject.Inject;
import java.io.IOException;

public class IntrospectionRegisterHandler implements IBootstrapHandler {

    private final IGraphQLDocumentManager manager;

    @Inject
    public IntrospectionRegisterHandler(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean execute(IPipelineContext context) throws IOException {
        manager.registerInputStream(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/preset.gql"));
        manager.registerInputStream(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/introspectionTypes.gql"));
        return false;
    }
}
