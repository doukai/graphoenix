package io.graphonix.grantlr.register;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlEnumManager {

    Map<String, GraphqlParser.EnumTypeDefinitionContext> register(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);
}
