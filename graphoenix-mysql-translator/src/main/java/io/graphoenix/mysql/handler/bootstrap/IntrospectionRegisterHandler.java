package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import java.io.IOException;

public class IntrospectionRegisterHandler implements IBootstrapHandler {

    @Override
    public boolean execute(IPipelineContext context) throws IOException {
        IGraphQLDocumentManager manager = context.getManager();
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/preset.gql"));
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/introspectionTypes.gql"));
        return false;
    }
}
