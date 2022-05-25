package io.graphoenix.json.schema.translator;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JsonSchemaTranslator {

    private final GraphQLConfig graphQLConfig;

    private final IGraphQLDocumentManager manager;

    @Inject
    public JsonSchemaTranslator(GraphQLConfig graphQLConfig,
                                IGraphQLDocumentManager manager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
    }
}
