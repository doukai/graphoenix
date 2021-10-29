package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphqlDocumentManager {

    public void registerDocument(String graphql);

    public void registerDocument(InputStream inputStream) throws IOException;

    public void registerDocument(GraphqlParser.DocumentContext documentContext);

    public GraphqlParser.OperationTypeContext getOperationType(String graphql);

    public GraphqlParser.OperationTypeContext getOperationType(GraphqlParser.DocumentContext documentContext);

    public void registerFragment(String graphql);

    public boolean isScaLar(String name);

    public boolean isInnerScalar(String name);

    public boolean isEnum(String name);

    public boolean isObject(String name);

    public boolean isInputObject(String name);

    public boolean isOperation(String name);

    public GraphqlParser.SchemaDefinitionContext getSchema();

    public Optional<GraphqlParser.DirectiveDefinitionContext> getDirective(String name);

    public Optional<GraphqlParser.ScalarTypeDefinitionContext> getScaLar(String name);

    public Optional<GraphqlParser.EnumTypeDefinitionContext> getEnum(String name);

    public Optional<GraphqlParser.ObjectTypeDefinitionContext> getObject(String name);

    public Optional<GraphqlParser.InputObjectTypeDefinitionContext> getInputObject(String name);

    public Stream<GraphqlParser.DirectiveDefinitionContext> getDirectives();

    public Stream<GraphqlParser.EnumTypeDefinitionContext> getEnums();

    public Stream<GraphqlParser.ObjectTypeDefinitionContext> getObjects();

    public Stream<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjects();

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getOperation(String name);

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getQueryOperationTypeDefinition();

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getMutationOperationTypeDefinition();

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getSubscriptionOperationTypeDefinition();

    public Optional<GraphqlParser.FieldDefinitionContext> getQueryOperationFieldDefinitionContext(String typeName, boolean list);

    public Optional<String> getObjectFieldTypeName(String typeName, String fieldName);

    public Optional<GraphqlParser.FieldDefinitionContext> getObjectFieldDefinitionContext(String typeName, String fieldName);

    public Optional<GraphqlParser.FragmentDefinitionContext> getObjectFragmentDefinitionContext(String typeName, String fragmentName);

    public Optional<String> getQueryOperationTypeName();

    public boolean isQueryOperationType(String typeName);

    public Optional<String> getMutationOperationTypeName();

    public boolean isMutationOperationType(String typeName);

    public Optional<String> getSubscriptionOperationTypeName();

    public boolean isSubscriptionOperationType(String typeName);

    public Optional<GraphqlParser.FieldDefinitionContext> getObjectTypeIDFieldDefinition(String objectTypeName);

    public Optional<GraphqlParser.FieldDefinitionContext> getMapFromFieldDefinition(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    public Optional<GraphqlParser.ValueWithVariableContext> getMapFromValueWithVariableFromArguments(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                                     GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                     GraphqlParser.ArgumentsContext parentArgumentsContext);

    public Optional<GraphqlParser.ValueWithVariableContext> getMapFromValueWithVariableFromObjectFieldWithVariable(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                                   GraphqlParser.ObjectValueWithVariableContext parentObjectValueWithVariableContext);

    public Optional<GraphqlParser.ValueContext> getMapFromValueFromObjectField(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                               GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                               GraphqlParser.ObjectValueContext parentObjectValueContext);

    public Optional<GraphqlParser.FieldDefinitionContext> getMapToFieldDefinition(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    public Optional<GraphqlParser.ValueWithVariableContext> getMapToValueWithVariableFromObjectFieldWithVariable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                                 GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext);

    public Optional<GraphqlParser.ValueContext> getMapToValueFromObjectField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                             GraphqlParser.ObjectValueContext objectValueContext);

    public Optional<GraphqlParser.ArgumentContext> getMapWithTypeArgument(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    public Optional<String> getMapWithTypeName(GraphqlParser.ArgumentContext argumentContext);

    public Optional<String> getMapWithTypeFromFieldName(GraphqlParser.ArgumentContext argumentContext);

    public Optional<String> getMapWithTypeToFieldName(GraphqlParser.ArgumentContext argumentContext);

    public Optional<GraphqlParser.FieldDefinitionContext> getMapWithTypeFromFieldDefinition(GraphqlParser.ArgumentContext argumentContext);

    public Optional<GraphqlParser.FieldDefinitionContext> getMapWithTypeToFieldDefinition(GraphqlParser.ArgumentContext argumentContext);

    public Optional<String> getObjectTypeIDFieldName(String objectTypeName);

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromArgumentsDefinitionContext(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext,
                                                                                                                     GraphqlParser.ArgumentContext argumentContext);

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                                                                           GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext);

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                                                                           GraphqlParser.ObjectFieldContext objectFieldContext);

    public Optional<GraphqlParser.ArgumentContext> getArgumentFromInputValueDefinition(GraphqlParser.ArgumentsContext argumentsContext,
                                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getObjectFieldWithVariableFromInputValueDefinition(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                                                                     GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    public Optional<GraphqlParser.ObjectFieldContext> getObjectFieldFromInputValueDefinition(GraphqlParser.ObjectValueContext objectValueContext,
                                                                                             GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    public Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinitionFromInputValueDefinition(GraphqlParser.TypeContext typeContext,
                                                                                                     GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    public Optional<GraphqlParser.ValueContext> getDefaultValueFromInputValueDefinition(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    public Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinitionFromOperationTypeDefinitionContext(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext,
                                                                                                               GraphqlParser.SelectionContext selectionContext);

    public Optional<GraphqlParser.ArgumentContext> getIDArgument(GraphqlParser.TypeContext typeContext,
                                                                 GraphqlParser.ArgumentsContext argumentsContext);

    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getIDObjectFieldWithVariable(GraphqlParser.TypeContext typeContext,
                                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext);

    public Optional<GraphqlParser.ObjectFieldContext> getIDObjectField(GraphqlParser.TypeContext typeContext,
                                                                       GraphqlParser.ObjectValueContext objectValueContext);

    public String getFieldTypeName(GraphqlParser.TypeContext typeContext);

    public boolean fieldTypeIsList(GraphqlParser.TypeContext typeContext);

    public boolean fieldTypeIsNonNull(GraphqlParser.TypeContext typeContext);

    public boolean fieldTypeIsNonNullList(GraphqlParser.TypeContext typeContext);
}
