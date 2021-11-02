package io.graphoenix.graphql.builder.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.document.Document;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.bootstrap.document.IDocumentBuildHandler;

@AutoService(IDocumentBuildHandler.class)
public class DocumentBuildHandler implements IDocumentBuildHandler {

    @Override
    public Void transform(IGraphqlDocumentManager manager, Void object) {
        return null;
    }

    @Override
    public void process(IGraphqlDocumentManager manager) throws Exception {
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/preset.gql"));
        Document document = new DocumentBuilder(manager).buildDocument();
        manager.registerDocument(document.toString());
    }
}
