package io.graphonix.grantlr.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlInputObjectManager {
    Map<String, GraphqlParser.InputObjectTypeDefinitionContext> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);
}
