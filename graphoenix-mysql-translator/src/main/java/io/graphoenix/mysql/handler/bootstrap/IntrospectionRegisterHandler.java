package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

import java.io.IOException;

public class IntrospectionRegisterHandler implements IBootstrapHandler {

    @Override
    public Void transform(IGraphqlDocumentManager manager, Object object) throws IOException {
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/preset.gql"));
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/introspectionTypes.gql"));
        return null;
    }
}
