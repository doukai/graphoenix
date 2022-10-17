package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.constant.Hammurabi;

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

    void registerFileByName(String graphqlFileName) throws IOException;

    void registerFile(File graphqlFile) throws IOException;

    void registerPathByName(String graphqlPathName) throws IOException, URISyntaxException;

    void registerPath(Path graphqlPath) throws IOException;

    void mergePath(Path graphqlPath) throws IOException;

    void registerDocument(GraphqlParser.DocumentContext documentContext);

    void mergeDocument(String graphQL);

    void mergeDocument(GraphqlParser.DocumentContext documentContext);

    GraphqlParser.OperationTypeContext getOperationType(String graphql);

    Stream<GraphqlParser.VariableDefinitionContext> getOperationTypeVariables(String graphql);

    GraphqlParser.OperationTypeContext getOperationType(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Stream<GraphqlParser.VariableDefinitionContext> getOperationTypeVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Hammurabi.MutationType getMutationType(GraphqlParser.SelectionContext selectionContext);

    void registerFragment(String graphql);

    boolean isScalar(String name);

    boolean isInnerScalar(String name);

    boolean isEnum(String name);

    boolean isObject(String name);

    boolean isInterface(String name);

    boolean isUnion(String name);

    boolean isInputObject(String name);

    boolean isOperation(String name);

    boolean isContainerType(String objectTypeName);

    boolean isNotContainerType(String objectTypeName);

    boolean isInvokeField(String objectTypeName, String name);

    boolean isNotInvokeField(String objectTypeName, String name);

    boolean isGrpcField(String objectTypeName, String name);

    boolean isNotGrpcField(String objectTypeName, String name);

    boolean isFunctionField(String objectTypeName, String name);

    boolean isNotFunctionField(String objectTypeName, String name);

    boolean isConnectionField(String objectTypeName, String name);

    boolean isNotConnectionField(String objectTypeName, String name);

    boolean isInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isGrpcField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotGrpcField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    boolean isNotConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext);

    GraphqlParser.SchemaDefinitionContext getSchema();

    Optional<GraphqlParser.DirectiveDefinitionContext> getDirective(String name);

    Optional<GraphqlParser.ScalarTypeDefinitionContext> getScaLar(String name);

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

    Optional<GraphqlParser.OperationTypeDefinitionContext> getOperation(String name);

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

    Optional<GraphqlParser.FieldDefinitionContext> getObjectTypeIDFieldDefinition(String objectTypeName);

    Optional<String> getObjectTypeIDFieldName(String objectTypeName);

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

    Optional<GraphqlParser.ObjectFieldContext> getIDObjectField(GraphqlParser.TypeContext typeContext,
                                                                GraphqlParser.ObjectValueContext objectValueContext);

    String getFieldTypeName(GraphqlParser.TypeContext typeContext);

    boolean fieldTypeIsList(GraphqlParser.TypeContext typeContext);

    void clearAll();
}
