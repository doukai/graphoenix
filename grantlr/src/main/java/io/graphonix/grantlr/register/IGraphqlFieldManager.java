package io.graphonix.grantlr.register;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public interface IGraphqlFieldManager {

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);
}
