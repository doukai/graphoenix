package io.graphonix.grantlr.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlInputValueManager {

    Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    Map<String, GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName);
}
