package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphqlSchemaManager;

public class GraphqlSchemaManager implements IGraphqlSchemaManager {

    private GraphqlParser.SchemaDefinitionContext schemaDefinitionContext;

    @Override
    public void register(GraphqlParser.SchemaDefinitionContext schemaDefinitionContext) {
        this.schemaDefinitionContext = schemaDefinitionContext;
    }

    @Override
    public GraphqlParser.SchemaDefinitionContext getSchemaDefinitionContext() {
        return schemaDefinitionContext;
    }
}
