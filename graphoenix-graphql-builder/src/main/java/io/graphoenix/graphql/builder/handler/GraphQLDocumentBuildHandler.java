package io.graphoenix.graphql.builder.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.graphql.builder.schema.GraphQLDocumentBuilder;
import io.graphoenix.graphql.generator.document.Document;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.bootstrap.IGraphQLDocumentBuildHandler;

@AutoService(IGraphQLDocumentBuildHandler.class)
public class GraphQLDocumentBuildHandler implements IGraphQLDocumentBuildHandler {

    @Override
    public Void transform(IGraphqlDocumentManager manager, Void object) {
        return null;
    }

    @Override
    public void process(IGraphqlDocumentManager manager) throws Exception {
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/preset.gql"));
        Document document = new GraphQLDocumentBuilder(manager).buildDocument();
        manager.registerDocument(document.toString());
    }
}
