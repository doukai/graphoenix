package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLFieldManager {

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    Stream<GraphqlParser.FieldDefinitionContext> getFieldDefinitions(String objectTypeName);

    Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinition(String objectTypeName, String fieldName);
}
