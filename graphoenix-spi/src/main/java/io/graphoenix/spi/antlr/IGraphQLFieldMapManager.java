package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Optional;

public interface IGraphQLFieldMapManager {

    void registerFieldMaps();

    void registerMap(String objectTypeName,
                     String fieldName,
                     GraphqlParser.FieldDefinitionContext from,
                     GraphqlParser.FieldDefinitionContext to);

    void registerMap(String objectTypeName,
                     String fieldName,
                     GraphqlParser.FieldDefinitionContext from,
                     GraphqlParser.ObjectTypeDefinitionContext withType,
                     GraphqlParser.FieldDefinitionContext withFrom,
                     GraphqlParser.FieldDefinitionContext withTo,
                     GraphqlParser.FieldDefinitionContext to);

    Optional<GraphqlParser.FieldDefinitionContext> getFromFieldDefinition(String objectTypeName, String fieldName);

    Optional<GraphqlParser.FieldDefinitionContext> getToFieldDefinition(String objectTypeName, String fieldName);

    boolean mapWithType(String objectTypeName, String fieldName);

    Optional<GraphqlParser.ObjectTypeDefinitionContext> getWithObjectTypeDefinition(String objectTypeName, String fieldName);

    Optional<GraphqlParser.FieldDefinitionContext> getWithFromFieldDefinition(String objectTypeName, String fieldName);

    Optional<GraphqlParser.FieldDefinitionContext> getWithToFieldDefinition(String objectTypeName, String fieldName);

    Optional<GraphqlParser.ValueWithVariableContext> getMapFromValueWithVariableFromArguments(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                              GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                              GraphqlParser.ArgumentsContext parentArgumentsContext);

    Optional<GraphqlParser.ValueWithVariableContext> getMapFromValueWithVariableFromObjectFieldWithVariable(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                                            GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                            GraphqlParser.ObjectValueWithVariableContext parentObjectValueWithVariableContext);

    Optional<GraphqlParser.ValueContext> getMapFromValueFromObjectField(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                        GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                        GraphqlParser.ObjectValueContext parentObjectValueContext);

    Optional<GraphqlParser.ValueWithVariableContext> getMapToValueWithVariableFromObjectFieldWithVariable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                          GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext);

    Optional<GraphqlParser.ValueContext> getMapToValueFromObjectField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                      GraphqlParser.ObjectValueContext objectValueContext);

    void clear();
}
