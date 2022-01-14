package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

public interface IGraphQLSchemaManager {

    void register(GraphqlParser.SchemaDefinitionContext schemaDefinitionContext);

    GraphqlParser.SchemaDefinitionContext getSchemaDefinitionContext();

    void clear();
}
