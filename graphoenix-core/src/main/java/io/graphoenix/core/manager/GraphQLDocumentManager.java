package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class GraphQLDocumentManager implements IGraphQLDocumentManager {

    private final IGraphQLOperationManager graphQLOperationManager;

    private final IGraphQLSchemaManager graphQLSchemaManager;

    private final IGraphQLDirectiveManager graphQLDirectiveManager;

    private final IGraphQLObjectManager graphQLObjectManager;

    private final IGraphQLInterfaceManager graphQLInterfaceManager;

    private final IGraphQLUnionManager graphQLUnionManager;

    private final IGraphQLFieldManager graphQLFieldManager;

    private final IGraphQLInputObjectManager graphQLInputObjectManager;

    private final IGraphQLInputValueManager graphQLInputValueManager;

    private final IGraphQLEnumManager graphQLEnumManager;

    private final IGraphQLScalarManager graphQLScalarManager;

    private final IGraphQLFragmentManager graphQLFragmentManager;

    public GraphQLDocumentManager(IGraphQLOperationManager graphQLOperationManager,
                                  IGraphQLSchemaManager graphQLSchemaManager,
                                  IGraphQLDirectiveManager graphQLDirectiveManager,
                                  IGraphQLObjectManager graphQLObjectManager,
                                  IGraphQLInterfaceManager graphQLInterfaceManager,
                                  IGraphQLUnionManager graphQLUnionManager,
                                  IGraphQLFieldManager graphQLFieldManager,
                                  IGraphQLInputObjectManager graphQLInputObjectManager,
                                  IGraphQLInputValueManager graphQLInputValueManager,
                                  IGraphQLEnumManager graphQLEnumManager,
                                  IGraphQLScalarManager graphQLScalarManager,
                                  IGraphQLFragmentManager graphQLFragmentManager) {
        this.graphQLOperationManager = graphQLOperationManager;
        this.graphQLSchemaManager = graphQLSchemaManager;
        this.graphQLDirectiveManager = graphQLDirectiveManager;
        this.graphQLObjectManager = graphQLObjectManager;
        this.graphQLInterfaceManager = graphQLInterfaceManager;
        this.graphQLUnionManager = graphQLUnionManager;
        this.graphQLFieldManager = graphQLFieldManager;
        this.graphQLInputObjectManager = graphQLInputObjectManager;
        this.graphQLInputValueManager = graphQLInputValueManager;
        this.graphQLEnumManager = graphQLEnumManager;
        this.graphQLScalarManager = graphQLScalarManager;
        this.graphQLFragmentManager = graphQLFragmentManager;
    }

    @Override
    public void registerGraphQL(String graphQL) {
        registerDocument(DOCUMENT_UTIL.graphqlToDocument(graphQL));
    }

    @Override
    public void registerInputStream(InputStream inputStream) throws IOException {
        registerDocument(DOCUMENT_UTIL.graphqlToDocument(inputStream));
    }

    @Override
    public void registerFileByName(String graphqlFileName) throws IOException {
        if (Files.exists(Path.of(graphqlFileName))) {
            registerFile(new File(graphqlFileName));
        } else {
            registerInputStream(this.getClass().getClassLoader().getResourceAsStream(graphqlFileName));
        }
    }

    @Override
    public void registerFile(File graphqlFile) throws IOException {
        registerDocument(DOCUMENT_UTIL.graphqlFileToDocument(graphqlFile));
    }

    @Override
    public void registerPathByName(String graphqlPathName) throws IOException, URISyntaxException {
        if (Files.exists(Path.of(graphqlPathName))) {
            registerPath(Path.of(graphqlPathName));
        } else {
            registerPath(Path.of(Objects.requireNonNull(this.getClass().getClassLoader().getResource(graphqlPathName)).toURI()));
        }
    }

    @Override
    public void registerPath(Path graphqlPath) throws IOException {
        registerDocument(DOCUMENT_UTIL.graphqlPathToDocument(graphqlPath));
    }

    @Override
    public void registerDocument(GraphqlParser.DocumentContext documentContext) {
        documentContext.definition().forEach(this::registerDefinition);
    }

    @Override
    public GraphqlParser.OperationTypeContext getOperationType(String graphql) {
        return getOperationType(DOCUMENT_UTIL.graphqlToDocument(graphql));
    }

    @Override
    public Stream<GraphqlParser.VariableDefinitionContext> getOperationTypeVariables(String graphql) {
        return getOperationTypeVariables(DOCUMENT_UTIL.graphqlToDocument(graphql));
    }

    @Override
    public GraphqlParser.OperationTypeContext getOperationType(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream()
                .filter(definitionContext -> definitionContext.operationDefinition() != null)
                .findFirst()
                .map(definitionContext -> definitionContext.operationDefinition().operationType())
                .orElse(null);
    }

    @Override
    public Stream<GraphqlParser.VariableDefinitionContext> getOperationTypeVariables(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream()
                .filter(definitionContext -> definitionContext.operationDefinition() != null)
                .findFirst()
                .map(definitionContext -> definitionContext.operationDefinition().variableDefinitions().variableDefinition().stream())
                .orElse(Stream.empty());
    }

    @Override
    public void registerFragment(String graphql) {
        DOCUMENT_UTIL.graphqlToDocument(graphql).definition().stream()
                .filter(definitionContext -> definitionContext.fragmentDefinition() != null)
                .forEach(this::registerDefinition);
    }

    protected void registerDefinition(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.typeSystemDefinition() != null) {
            registerSystemDefinition(definitionContext.typeSystemDefinition());
        } else if (definitionContext.fragmentDefinition() != null) {
            graphQLFragmentManager.register(definitionContext.fragmentDefinition());
        }
    }

    protected void registerSystemDefinition(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.schemaDefinition() != null) {
            typeSystemDefinitionContext.schemaDefinition().operationTypeDefinition().forEach(this::registerOperationType);
            graphQLSchemaManager.register(typeSystemDefinitionContext.schemaDefinition());
        } else if (typeSystemDefinitionContext.typeDefinition() != null) {
            registerTypeDefinition(typeSystemDefinitionContext.typeDefinition());
        } else if (typeSystemDefinitionContext.directiveDefinition() != null) {
            graphQLDirectiveManager.register(typeSystemDefinitionContext.directiveDefinition());
        }
    }

    protected void registerOperationType(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        graphQLOperationManager.register(operationTypeDefinitionContext);
    }

    protected void registerTypeDefinition(GraphqlParser.TypeDefinitionContext typeDefinitionContext) {

        if (typeDefinitionContext.scalarTypeDefinition() != null) {
            graphQLScalarManager.register(typeDefinitionContext.scalarTypeDefinition());
        } else if (typeDefinitionContext.enumTypeDefinition() != null) {
            graphQLEnumManager.register(typeDefinitionContext.enumTypeDefinition());
        } else if (typeDefinitionContext.objectTypeDefinition() != null) {
            graphQLObjectManager.register(typeDefinitionContext.objectTypeDefinition());
            graphQLFieldManager.register(typeDefinitionContext.objectTypeDefinition());
        } else if (typeDefinitionContext.interfaceTypeDefinition() != null) {
            graphQLInterfaceManager.register(typeDefinitionContext.interfaceTypeDefinition());
            graphQLFieldManager.register(typeDefinitionContext.interfaceTypeDefinition());
        } else if (typeDefinitionContext.unionTypeDefinition() != null) {
            graphQLUnionManager.register(typeDefinitionContext.unionTypeDefinition());
        } else if (typeDefinitionContext.inputObjectTypeDefinition() != null) {
            graphQLInputObjectManager.register(typeDefinitionContext.inputObjectTypeDefinition());
            graphQLInputValueManager.register(typeDefinitionContext.inputObjectTypeDefinition());
        }
    }

    @Override
    public boolean isScalar(String name) {
        return graphQLScalarManager.isScalar(name);
    }

    @Override
    public boolean isEnum(String name) {
        return graphQLEnumManager.isEnum(name);
    }

    @Override
    public boolean isObject(String name) {
        return graphQLObjectManager.isObject(name);
    }

    @Override
    public boolean isInterface(String name) {
        return graphQLInterfaceManager.isInterface(name);
    }

    @Override
    public boolean isUnion(String name) {
        return graphQLUnionManager.isUnion(name);
    }

    @Override
    public boolean isInputObject(String name) {
        return graphQLInputObjectManager.isInputObject(name);
    }

    @Override
    public boolean isOperation(String name) {
        return graphQLOperationManager.isOperation(name);
    }

    @Override
    public boolean isInvokeField(String objectTypeName, String name) {
        return graphQLFieldManager.isInvokeField(objectTypeName, name);
    }

    @Override
    public boolean isNotInvokeField(String objectTypeName, String name) {
        return graphQLFieldManager.isNotInvokeField(objectTypeName, name);
    }

    @Override
    public GraphqlParser.SchemaDefinitionContext getSchema() {
        return graphQLSchemaManager.getSchemaDefinitionContext();
    }

    @Override
    public Optional<GraphqlParser.DirectiveDefinitionContext> getDirective(String name) {
        return graphQLDirectiveManager.getDirectiveDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.ScalarTypeDefinitionContext> getScaLar(String name) {
        return graphQLScalarManager.getScalarTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.EnumTypeDefinitionContext> getEnum(String name) {
        return graphQLEnumManager.getEnumTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.ObjectTypeDefinitionContext> getObject(String name) {
        return graphQLObjectManager.getObjectTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getField(String objectName, String name) {
        return graphQLFieldManager.getFieldDefinition(objectName, name);
    }

    @Override
    public Optional<GraphqlParser.InterfaceTypeDefinitionContext> getInterface(String name) {
        return graphQLInterfaceManager.getInterfaceTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.UnionTypeDefinitionContext> getUnion(String name) {
        return graphQLUnionManager.getUnionTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.InputObjectTypeDefinitionContext> getInputObject(String name) {
        return graphQLInputObjectManager.getInputObjectTypeDefinition(name);
    }

    @Override
    public Stream<GraphqlParser.DirectiveDefinitionContext> getDirectives() {
        return graphQLDirectiveManager.getDirectiveDefinitions();
    }

    @Override
    public Stream<GraphqlParser.ScalarTypeDefinitionContext> getScalars() {
        return graphQLScalarManager.getScalarTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.EnumTypeDefinitionContext> getEnums() {
        return graphQLEnumManager.getEnumTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.ObjectTypeDefinitionContext> getObjects() {
        return graphQLObjectManager.getObjectTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.FieldDefinitionContext> getFields(String objectName) {
        return graphQLFieldManager.getFieldDefinitions(objectName);
    }

    @Override
    public Stream<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaces() {
        return graphQLInterfaceManager.getInterfaceTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.UnionTypeDefinitionContext> getUnions() {
        return graphQLUnionManager.getUnionTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjects() {
        return graphQLInputObjectManager.getInputObjectTypeDefinitions();
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getOperation(String name) {
        return graphQLOperationManager.getOperationTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getQueryOperationTypeDefinition() {
        return graphQLOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().QUERY() != null).findFirst();
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getMutationOperationTypeDefinition() {
        return graphQLOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().MUTATION() != null).findFirst();
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getSubscriptionOperationTypeDefinition() {
        return graphQLOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().SUBSCRIPTION() != null).findFirst();
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getObjectFieldDefinition(String typeName, String fieldName) {
        return graphQLFieldManager.getFieldDefinition(typeName, fieldName);
    }

    @Override
    public Optional<GraphqlParser.FragmentDefinitionContext> getObjectFragmentDefinition(String typeName, String fragmentName) {
        return graphQLFragmentManager.getFragmentDefinition(typeName, fragmentName);
    }

    @Override
    public Optional<String> getQueryOperationTypeName() {
        Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = getQueryOperationTypeDefinition();
        return queryOperationTypeDefinition.map(operationTypeDefinitionContext -> operationTypeDefinitionContext.typeName().name().getText());
    }

    @Override
    public boolean isQueryOperationType(String typeName) {
        Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = getQueryOperationTypeDefinition();
        return queryOperationTypeDefinition.isPresent() && queryOperationTypeDefinition.get().typeName().name().getText().equals(typeName);
    }

    @Override
    public Optional<String> getMutationOperationTypeName() {
        Optional<GraphqlParser.OperationTypeDefinitionContext> mutationOperationTypeDefinition = getMutationOperationTypeDefinition();
        return mutationOperationTypeDefinition.map(operationTypeDefinitionContext -> operationTypeDefinitionContext.typeName().name().getText());
    }

    @Override
    public boolean isMutationOperationType(String typeName) {
        Optional<GraphqlParser.OperationTypeDefinitionContext> mutationOperationTypeDefinition = getMutationOperationTypeDefinition();
        return mutationOperationTypeDefinition.isPresent() && mutationOperationTypeDefinition.get().typeName().name().getText().equals(typeName);
    }

    @Override
    public Optional<String> getSubscriptionOperationTypeName() {
        Optional<GraphqlParser.OperationTypeDefinitionContext> subscriptionOperationTypeDefinition = getSubscriptionOperationTypeDefinition();
        return subscriptionOperationTypeDefinition.map(operationTypeDefinitionContext -> operationTypeDefinitionContext.typeName().name().getText());
    }

    @Override
    public boolean isSubscriptionOperationType(String typeName) {
        Optional<GraphqlParser.OperationTypeDefinitionContext> subscriptionOperationTypeDefinition = getSubscriptionOperationTypeDefinition();
        return subscriptionOperationTypeDefinition.isPresent() && subscriptionOperationTypeDefinition.get().typeName().name().getText().equals(typeName);
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getObjectTypeIDFieldDefinition(String objectTypeName) {
        return graphQLFieldManager.getFieldDefinitions(objectTypeName)
                .filter(fieldDefinitionContext -> !fieldTypeIsList(fieldDefinitionContext.type()))
                .filter(fieldDefinitionContext -> getFieldTypeName(fieldDefinitionContext.type()).equals("ID")).findFirst();
    }

    @Override
    public Optional<String> getObjectTypeIDFieldName(String objectTypeName) {
        return graphQLFieldManager.getFieldDefinitions(objectTypeName)
                .filter(fieldDefinitionContext -> !fieldTypeIsList(fieldDefinitionContext.type()))
                .filter(fieldDefinitionContext -> getFieldTypeName(fieldDefinitionContext.type()).equals("ID")).findFirst()
                .map(fieldDefinitionContext -> fieldDefinitionContext.name().getText());
    }

    @Override
    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromArgumentsDefinitionContext(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return argumentsDefinitionContext.inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(argumentContext.name().getText())).findFirst();
    }

    @Override
    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldWithVariableContext.name().getText())).findFirst();
    }

    @Override
    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldContext.name().getText())).findFirst();
    }

    @Override
    public Optional<GraphqlParser.ArgumentContext> getArgumentFromInputValueDefinition(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    @Override
    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getObjectFieldWithVariableFromInputValueDefinition(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream().filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    @Override
    public Optional<GraphqlParser.ObjectFieldContext> getObjectFieldFromInputValueDefinition(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return objectValueContext.objectField().stream().filter(objectFieldContext -> objectFieldContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinitionFromInputValueDefinition(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return graphQLFieldManager.getFieldDefinitions(getFieldTypeName(typeContext))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    @Override
    public Optional<GraphqlParser.ValueContext> getDefaultValueFromInputValueDefinition(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.defaultValue() != null) {
            return Optional.of(inputValueDefinitionContext.defaultValue().value());
        }
        return Optional.empty();
    }

    @Override
    public Optional<GraphqlParser.ArgumentContext> getIDArgument(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition.flatMap(fieldDefinitionContext -> argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText())).findFirst());
    }

    @Override
    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getIDObjectFieldWithVariable(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition.flatMap(fieldDefinitionContext -> objectValueWithVariableContext.objectFieldWithVariable().stream().filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText())).findFirst());
    }

    @Override
    public Optional<GraphqlParser.ObjectFieldContext> getIDObjectField(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition.flatMap(fieldDefinitionContext -> objectValueContext.objectField().stream().filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText())).findFirst());
    }

    @Override
    public String getFieldTypeName(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            return typeContext.typeName().name().getText();
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return typeContext.nonNullType().typeName().name().getText();
            } else if (typeContext.nonNullType().listType() != null) {
                return getFieldTypeName(typeContext.nonNullType().listType().type());
            }
        } else if (typeContext.listType() != null) {
            return getFieldTypeName(typeContext.listType().type());
        }
        return null;
    }

    @Override
    public boolean fieldTypeIsList(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            return false;
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return false;
            } else return typeContext.nonNullType().listType() != null;
        } else return typeContext.listType() != null;
    }

    @Override
    public void clearAll() {
        graphQLOperationManager.clear();
        graphQLSchemaManager.clear();
        graphQLDirectiveManager.clear();
        graphQLObjectManager.clear();
        graphQLInterfaceManager.clear();
        graphQLUnionManager.clear();
        graphQLFieldManager.clear();
        graphQLInputObjectManager.clear();
        graphQLInputValueManager.clear();
        graphQLEnumManager.clear();
        graphQLScalarManager.clear();
        graphQLFragmentManager.clear();
    }
}
