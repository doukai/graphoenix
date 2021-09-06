package io.graphoenix.antlr.manager;

import graphql.parser.antlr.GraphqlParser;

public interface IGraphqlSchemaManager {

    void register(GraphqlParser.SchemaDefinitionContext schemaDefinitionContext);

    GraphqlParser.SchemaDefinitionContext getSchemaDefinitionContext();
}
