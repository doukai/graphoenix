package io.graphoenix.graphql.builder.handler.bootstrap;

import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.document.Document;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

public class DocumentBuildHandler implements IBootstrapHandler {

    private final String graphQLFileName;

    public DocumentBuildHandler(String graphQL) {
        this.graphQLFileName = graphQL;
    }

    @Override
    public Object transform(IGraphqlDocumentManager manager, Object object) {
        return null;
    }

    @Override
    public void process(IGraphqlDocumentManager manager) throws Exception {
        if (this.graphQLFileName != null) {
            manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream(graphQLFileName));
        }
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/preset.gql"));
        Document document = new DocumentBuilder(manager).buildDocument();
        manager.registerDocument(document.toString());
    }
}
