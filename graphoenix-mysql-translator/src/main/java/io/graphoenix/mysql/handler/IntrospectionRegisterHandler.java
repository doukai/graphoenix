package io.graphoenix.mysql.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.bootstrap.introspection.IIntrospectionRegisterHandler;

import java.io.IOException;

@AutoService(IIntrospectionRegisterHandler.class)
public class IntrospectionRegisterHandler implements IIntrospectionRegisterHandler {

    @Override
    public Void transform(IGraphqlDocumentManager manager, Void object) {
        return null;
    }

    @Override
    public void process(IGraphqlDocumentManager manager) throws IOException {
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/preset.gql"));
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/introspectionTypes.gql"));
    }
}
