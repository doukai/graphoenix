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

    boolean isFetchField(String objectTypeName, String fieldName);

    boolean isNotFetchField(String objectTypeName, String fieldName);

    boolean isFunctionField(String objectTypeName, String fieldName);

    boolean isNotFunctionField(String objectTypeName, String fieldName);

    boolean isConnectionField(String objectTypeName, String fieldName);

    boolean isNotConnectionField(String objectTypeName, String fieldName);

    boolean isInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isFetchField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotFetchField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    void clear();
}
