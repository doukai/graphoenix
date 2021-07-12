package io.graphonix.grantlr.register;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlScalarManager {

    Map<String, GraphqlParser.ScalarTypeDefinitionContext> register(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext);
}
