package io.graphonix.grantlr.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlObjectManager {

    Map<String, GraphqlParser.ObjectTypeDefinitionContext> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    GraphqlParser.ObjectTypeDefinitionContext getObjectTypeDefinition(String objectTypeName);
}
