package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.constant.Hammurabi;

import javax.annotation.processing.Filer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLDocumentManager {

    void registerGraphQL(String graphQL);

    void registerInputStream(InputStream inputStream) throws IOException;

    void registerFileByName(String graphqlFileName) throws IOException, URISyntaxException;

    void registerFileByName(String graphqlFileName, ClassLoader classLoader) throws IOException, URISyntaxException;

    void registerFile(File graphqlFile) throws IOException;

    void registerPathByName(String graphqlPathName) throws IOException, URISyntaxException;

    void registerPath(Path graphqlPath) throws IOException;

    void registerPathByName(String graphqlPathName, ClassLoader classLoader) throws IOException, URISyntaxException;

    void registerPathByName(String graphqlPathName, String resourcePath) throws IOException, URISyntaxException;

    void registerPathByName(String graphqlPathName, Filer filer) throws IOException, URISyntaxException;

    void mergePath(Path graphqlPath) throws IOException;

    void registerDocument(GraphqlParser.DocumentContext documentContext);

    void mergeDocument(String graphQL);

    void mergeDocument(GraphqlParser.DocumentContext documentContext);

    GraphqlParser.OperationTypeContext getOperationType(String graphql);

    Stream<GraphqlParser.VariableDefinitionContext> getOperationTypeVariables(String graphql);

    GraphqlParser.OperationTypeContext getOperationType(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Stream<GraphqlParser.VariableDefinitionContext> getOperationTypeVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Hammurabi.MutationType getMutationType(GraphqlParser.SelectionContext selectionContext);

    boolean mergeToList(GraphqlParser.SelectionContext selectionContext, String argumentName);

    void registerFragment(String graphql);

    boolean isScalar(String name);

    boolean isInnerScalar(String name);

    boolean isEnum(String name);

    boolean isObject(String name);

    boolean isInterface(String name);

    boolean isUnion(String name);

    boolean isInputObject(String name);

    boolean isOperation(String name);

    boolean isInvokeField(String objectTypeName, String name);

    boolean isNotInvokeField(String objectTypeName, String name);

    boolean isFetchField(String objectTypeName, String name);

    boolean isNotFetchField(String objectTypeName, String name);

    boolean isFunctionField(String objectTypeName, String name);

    boolean isNotFunctionField(String objectTypeName, String name);

    boolean isConnectionField(String objectTypeName, String name);

    boolean isNotConnectionField(String objectTypeName, String name);

    boolean isInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isFetchField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotFetchField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    GraphqlParser.SchemaDefinitionContext getSchema();

    Optional<GraphqlParser.DirectiveDefinitionContext> getDirective(String name);

    Optional<GraphqlParser.ScalarTypeDefinitionContext> getScalar(String name);

    Optional<GraphqlParser.EnumTypeDefinitionContext> getEnum(String name);

    Optional<GraphqlParser.ObjectTypeDefinitionContext> getObject(String name);

    Optional<GraphqlParser.FieldDefinitionContext> getField(String objectName, String name);

    Stream<GraphqlParser.FieldDefinitionContext> getFieldByDirective(String objectName, String directiveName);

    Optional<GraphqlParser.InterfaceTypeDefinitionContext> getInterface(String name);

    Optional<GraphqlParser.UnionTypeDefinitionContext> getUnion(String name);

    Optional<GraphqlParser.InputObjectTypeDefinitionContext> getInputObject(String name);

    Stream<GraphqlParser.DirectiveDefinitionContext> getDirectives();

    Stream<GraphqlParser.ScalarTypeDefinitionContext> getScalars();

    Stream<GraphqlParser.EnumTypeDefinitionContext> getEnums();

    Stream<GraphqlParser.ObjectTypeDefinitionContext> getObjects();

    Stream<GraphqlParser.FieldDefinitionContext> getFields(String objectName);

    Stream<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaces();

    Stream<GraphqlParser.UnionTypeDefinitionContext> getUnions();

    Stream<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjects();

    Optional<GraphqlParser.OperationTypeDefinitionContext> getOperationTypeDefinition(String name);

    Optional<GraphqlParser.OperationDefinitionContext> getOperationDefinition(String name);

    Stream<GraphqlParser.OperationDefinitionContext> getOperationDefinitions();

    Stream<GraphqlParser.OperationTypeDefinitionContext> getOperationTypeDefinition();

    Optional<GraphqlParser.OperationTypeDefinitionContext> getQueryOperationTypeDefinition();

    Optional<GraphqlParser.OperationTypeDefinitionContext> getMutationOperationTypeDefinition();

    Optional<GraphqlParser.OperationTypeDefinitionContext> getSubscriptionOperationTypeDefinition();

    Optional<GraphqlParser.FieldDefinitionContext> getObjectFieldDefinition(String typeName, String fieldName);

    Optional<GraphqlParser.FragmentDefinitionContext> getObjectFragmentDefinition(String typeName, String fragmentName);

    Stream<GraphqlParser.SelectionContext> fragmentUnzip(String typeName, GraphqlParser.SelectionContext selectionContext);

    Optional<String> getQueryOperationTypeName();

    boolean isQueryOperationType(String typeName);

    Optional<String> getMutationOperationTypeName();

    boolean isMutationOperationType(String typeName);

    Optional<String> getSubscriptionOperationTypeName();

    boolean isSubscriptionOperationType(String typeName);

    boolean isOperationType(String typeName);

    boolean isOperationType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    boolean isNotOperationType(String typeName);

    boolean isNotOperationType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    Optional<GraphqlParser.FieldDefinitionContext> getObjectTypeIDFieldDefinition(String objectTypeName);

    Optional<GraphqlParser.FieldDefinitionContext> getObjectTypeIsDeprecatedFieldDefinition(String objectTypeName);

    Optional<String> getObjectTypeIDFieldName(String objectTypeName);

    boolean isContainerType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    boolean isNotContainerType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    boolean isContainerType(GraphqlParser.TypeContext typeContext);

    boolean isNotContainerType(GraphqlParser.TypeContext typeContext);

    boolean hasClassName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    boolean hasClassName(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);

    boolean hasClassName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    boolean hasClassName(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    boolean hasClassName(GraphqlParser.TypeContext typeContext);

    boolean hasClassName(GraphqlParser.DirectivesContext directivesContext);

    boolean hasClassName(String typeName);

    Optional<String> getClassName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    Optional<String> getClassName(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);

    Optional<String> getClassName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    Optional<String> getClassName(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    Optional<String> getClassName(GraphqlParser.TypeContext typeContext);

    Optional<String> getClassName(GraphqlParser.DirectivesContext directivesContext);

    String getClassName(String typeName);

    boolean classExists(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    boolean classExists(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);

    boolean classExists(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    boolean classExists(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    boolean classExists(GraphqlParser.TypeContext typeContext);

    boolean classNotExists(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    boolean classNotExists(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);

    boolean classNotExists(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    boolean classNotExists(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    boolean classNotExists(GraphqlParser.TypeContext typeContext);

    boolean classExists(GraphqlParser.DirectivesContext directivesContext);

    Optional<String> getAnnotationName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    Optional<String> getAnnotationName(GraphqlParser.TypeContext typeContext);

    Optional<String> getAnnotationName(GraphqlParser.DirectivesContext directivesContext);

    String getAnnotationName(String typeName);

    Optional<String> getGrpcClassName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    Optional<String> getGrpcClassName(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);

    Optional<String> getGrpcClassName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    Optional<String> getGrpcClassName(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    Optional<String> getGrpcClassName(GraphqlParser.DirectivesContext directivesContext);

    Optional<String> getGrpcClassName(GraphqlParser.TypeContext typeContext);

    String getGrpcClassName(String typeName);

    Optional<String> getPackageName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    Optional<String> getPackageName(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);

    Optional<String> getPackageName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    Optional<String> getPackageName(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    Optional<String> getPackageName(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Optional<String> getPackageName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    Optional<String> getPackageName(GraphqlParser.DirectivesContext directivesContext);

    Optional<String> getPackageName(GraphqlParser.TypeContext typeContext);

    String getPackageName(String typeName);

    Optional<String> getGrpcPackageName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    Optional<String> getGrpcPackageName(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);

    Optional<String> getGrpcPackageName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    Optional<String> getGrpcPackageName(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    Optional<String> getGrpcPackageName(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Optional<String> getGrpcPackageName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    Optional<String> getGrpcPackageName(GraphqlParser.DirectivesContext directivesContext);

    Optional<String> getGrpcPackageName(GraphqlParser.TypeContext typeContext);

    String getGrpcPackageName(String typeName);

    String getProtocol(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    String getFrom(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    String getTo(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean getAnchor(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean hasWith(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    String getWithType(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    String getWithFrom(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    GraphqlParser.FieldDefinitionContext getWithFromObjectField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    String getWithTo(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    GraphqlParser.FieldDefinitionContext getWithToObjectField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromArgumentsDefinitionContext(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext,
                                                                                                              GraphqlParser.ArgumentContext argumentContext);

    Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                                                                    GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext);

    Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                                                                    GraphqlParser.ObjectFieldContext objectFieldContext);

    Optional<GraphqlParser.ArgumentContext> getArgumentFromInputValueDefinition(GraphqlParser.ArgumentsContext argumentsContext,
                                                                                GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    Optional<GraphqlParser.ObjectFieldWithVariableContext> getObjectFieldWithVariableFromInputValueDefinition(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                                                              GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    Optional<GraphqlParser.ObjectFieldContext> getObjectFieldFromInputValueDefinition(GraphqlParser.ObjectValueContext objectValueContext,
                                                                                      GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinitionFromInputValueDefinition(GraphqlParser.TypeContext typeContext,
                                                                                              GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    Optional<GraphqlParser.ValueContext> getDefaultValueFromInputValueDefinition(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext);

    Optional<GraphqlParser.ArgumentContext> getIDArgument(GraphqlParser.TypeContext typeContext,
                                                          GraphqlParser.ArgumentsContext argumentsContext);

    Optional<GraphqlParser.ObjectFieldWithVariableContext> getIDObjectFieldWithVariable(GraphqlParser.TypeContext typeContext,
                                                                                        GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext);

    Optional<GraphqlParser.ObjectFieldWithVariableContext> getIsDeprecatedObjectFieldWithVariable(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext);

    Optional<GraphqlParser.ObjectFieldContext> getIDObjectField(GraphqlParser.TypeContext typeContext,
                                                                GraphqlParser.ObjectValueContext objectValueContext);

    String getFieldTypeName(GraphqlParser.TypeContext typeContext);

    boolean fieldTypeIsList(GraphqlParser.TypeContext typeContext);

    Stream<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaces(GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext);

    Stream<GraphqlParser.DirectiveLocationContext> getDirectiveLocations(GraphqlParser.DirectiveLocationsContext directiveLocationsContext);

    void clearAll();
}
