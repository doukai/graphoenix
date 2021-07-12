package io.graphonix.grantlr.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlOperationManager {

    Map<String, GraphqlParser.OperationTypeDefinitionContext> register(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext);
}
