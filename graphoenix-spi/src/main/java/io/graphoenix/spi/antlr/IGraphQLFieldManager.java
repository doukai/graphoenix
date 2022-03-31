package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLFieldManager {

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    Stream<GraphqlParser.FieldDefinitionContext> getFieldDefinitions(String objectTypeName);

    Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinition(String objectTypeName, String fieldName);

    Stream<GraphqlParser.FieldDefinitionContext> getFieldDefinitionByDirective(String objectTypeName, String directiveName);

    boolean isInvokeField(String objectTypeName, String fieldName);

    boolean isNotInvokeField(String objectTypeName, String fieldName);

    boolean isFunctionField(String objectTypeName, String fieldName);

    boolean isNotFunctionField(String objectTypeName, String fieldName);

    boolean isConnectionField(String objectTypeName, String fieldName);

    boolean isNotConnectionField(String objectTypeName, String fieldName);

    void clear();
}
