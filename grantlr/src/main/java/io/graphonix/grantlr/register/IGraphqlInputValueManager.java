package io.graphonix.grantlr.register;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlInputValueManager {

    Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);
}
