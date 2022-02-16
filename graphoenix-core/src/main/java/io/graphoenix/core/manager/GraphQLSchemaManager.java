package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLSchemaManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

@ApplicationScoped
public class GraphQLSchemaManager implements IGraphQLSchemaManager {

    private GraphqlParser.SchemaDefinitionContext schemaDefinitionContext;

    @Override
    public void register(GraphqlParser.SchemaDefinitionContext schemaDefinitionContext) {
        this.schemaDefinitionContext = schemaDefinitionContext;
        Logger.debug("registered schema");
    }

    @Override
    public GraphqlParser.SchemaDefinitionContext getSchemaDefinitionContext() {
        return schemaDefinitionContext;
    }

    @Override
    public void clear() {
        schemaDefinitionContext = null;
        Logger.debug("clear schema");
    }
}
