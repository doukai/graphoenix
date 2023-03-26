package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;

public interface GeneratorHandler {

    String query(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    String mutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    String query(String graphQL);

    String mutation(String graphQL);

    String extension();
}
