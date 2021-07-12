package io.graphonix.grantlr.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlEnumManager {

    Map<String, GraphqlParser.EnumTypeDefinitionContext> register(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);
}
