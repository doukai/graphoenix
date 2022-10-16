package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.document.EnumType;
import io.graphoenix.core.document.InputObjectType;
import io.graphoenix.core.document.InterfaceType;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDirectiveManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLEnumManager;
import io.graphoenix.spi.antlr.IGraphQLFieldManager;
import io.graphoenix.spi.antlr.IGraphQLFragmentManager;
import io.graphoenix.spi.antlr.IGraphQLInputObjectManager;
import io.graphoenix.spi.antlr.IGraphQLInputValueManager;
import io.graphoenix.spi.antlr.IGraphQLInterfaceManager;
import io.graphoenix.spi.antlr.IGraphQLObjectManager;
import io.graphoenix.spi.antlr.IGraphQLOperationManager;
import io.graphoenix.spi.antlr.IGraphQLScalarManager;
import io.graphoenix.spi.antlr.IGraphQLSchemaManager;
import io.graphoenix.spi.antlr.IGraphQLUnionManager;
import io.graphoenix.spi.constant.Hammurabi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.FRAGMENT_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.DELETE_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.MutationType.DELETE;
import static io.graphoenix.spi.constant.Hammurabi.MutationType.MERGE;
import static io.graphoenix.spi.constant.Hammurabi.MutationType.UPDATE;
import static io.graphoenix.spi.constant.Hammurabi.UPDATE_DIRECTIVE_NAME;

@ApplicationScoped
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

    @Inject
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
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(graphqlFileName);
            if (inputStream != null) {
                registerInputStream(inputStream);
            }
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
            URL resource = this.getClass().getClassLoader().getResource(graphqlPathName);
            if (resource != null) {
                registerPath(Path.of(resource.toURI()));
            }
        }
    }

    @Override
    public void registerPath(Path graphqlPath) throws IOException {
        registerDocument(DOCUMENT_UTIL.graphqlPathToDocument(graphqlPath));
    }

    @Override
    public void mergePath(Path graphqlPath) throws IOException {
        mergeDocument(DOCUMENT_UTIL.graphqlPathToDocument(graphqlPath));
    }

    @Override
    public void registerDocument(GraphqlParser.DocumentContext documentContext) {
        documentContext.definition().forEach(this::registerDefinition);
    }

    @Override
    public void mergeDocument(String graphQL) {
        mergeDocument(DOCUMENT_UTIL.graphqlToDocument(graphQL));
    }

    @Override
    public void mergeDocument(GraphqlParser.DocumentContext documentContext) {
        documentContext.definition().forEach(this::mergeDefinition);
    }

    @Override
    public GraphqlParser.OperationTypeContext getOperationType(String graphql) {
        return getOperationType(DOCUMENT_UTIL.graphqlToOperation(graphql));
    }

    @Override
    public Stream<GraphqlParser.VariableDefinitionContext> getOperationTypeVariables(String graphql) {
        return getOperationTypeVariables(DOCUMENT_UTIL.graphqlToOperation(graphql));
    }

    @Override
    public GraphqlParser.OperationTypeContext getOperationType(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return operationDefinitionContext.operationType();
    }

    @Override
    public Stream<GraphqlParser.VariableDefinitionContext> getOperationTypeVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return operationDefinitionContext.variableDefinitions().variableDefinition().stream();
    }

    @Override
    public Hammurabi.MutationType getMutationType(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return Stream.ofNullable(operationDefinitionContext.directives())
                .map(directivesContext ->
                        directivesContext.directive().stream()
                                .filter(directiveContext -> directiveContext.name().getText().equals(UPDATE_DIRECTIVE_NAME))
                                .findFirst()
                                .map(directiveContext -> UPDATE)
                                .orElseGet(() ->
                                        directivesContext.directive().stream()
                                                .filter(directiveContext -> directiveContext.name().getText().equals(DELETE_DIRECTIVE_NAME))
                                                .findFirst()
                                                .map(directiveContext -> DELETE)
                                                .orElse(MERGE)
                                )
                )
                .findFirst()
                .orElse(MERGE);
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

    protected void mergeDefinition(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.typeSystemDefinition() != null) {
            mergeSystemDefinition(definitionContext.typeSystemDefinition());
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

    protected void mergeSystemDefinition(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.schemaDefinition() != null) {
            typeSystemDefinitionContext.schemaDefinition().operationTypeDefinition().forEach(this::registerOperationType);
            graphQLSchemaManager.register(typeSystemDefinitionContext.schemaDefinition());
        } else if (typeSystemDefinitionContext.typeDefinition() != null) {
            mergeTypeDefinition(typeSystemDefinitionContext.typeDefinition());
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

    protected void mergeTypeDefinition(GraphqlParser.TypeDefinitionContext typeDefinitionContext) {
        if (typeDefinitionContext.scalarTypeDefinition() != null) {
            graphQLScalarManager.register(typeDefinitionContext.scalarTypeDefinition());
        } else if (typeDefinitionContext.enumTypeDefinition() != null) {
            Optional<GraphqlParser.EnumTypeDefinitionContext> original = getEnum(typeDefinitionContext.enumTypeDefinition().name().getText());
            if (original.isPresent()) {
                GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext = DOCUMENT_UTIL.graphqlToEnumTypeDefinition(EnumType.merge(original.get(), typeDefinitionContext.enumTypeDefinition()).toString());
                graphQLEnumManager.register(enumTypeDefinitionContext);
            } else {
                graphQLEnumManager.register(typeDefinitionContext.enumTypeDefinition());
            }
        } else if (typeDefinitionContext.objectTypeDefinition() != null) {
            Optional<GraphqlParser.ObjectTypeDefinitionContext> original = getObject(typeDefinitionContext.objectTypeDefinition().name().getText());
            if (original.isPresent()) {
                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = DOCUMENT_UTIL.graphqlToObjectTypeDefinition(ObjectType.merge(original.get(), typeDefinitionContext.objectTypeDefinition()).toString());
                graphQLObjectManager.register(objectTypeDefinitionContext);
                graphQLFieldManager.register(objectTypeDefinitionContext);
            } else {
                graphQLObjectManager.register(typeDefinitionContext.objectTypeDefinition());
                graphQLFieldManager.register(typeDefinitionContext.objectTypeDefinition());
            }
        } else if (typeDefinitionContext.interfaceTypeDefinition() != null) {
            Optional<GraphqlParser.InterfaceTypeDefinitionContext> original = getInterface(typeDefinitionContext.interfaceTypeDefinition().name().getText());
            if (original.isPresent()) {
                GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext = DOCUMENT_UTIL.graphqlToInterfaceTypeDefinition(InterfaceType.merge(original.get(), typeDefinitionContext.interfaceTypeDefinition()).toString());
                graphQLInterfaceManager.register(interfaceTypeDefinitionContext);
                graphQLFieldManager.register(interfaceTypeDefinitionContext);
            } else {
                graphQLInterfaceManager.register(typeDefinitionContext.interfaceTypeDefinition());
                graphQLFieldManager.register(typeDefinitionContext.interfaceTypeDefinition());
            }
        } else if (typeDefinitionContext.unionTypeDefinition() != null) {
            graphQLUnionManager.register(typeDefinitionContext.unionTypeDefinition());
        } else if (typeDefinitionContext.inputObjectTypeDefinition() != null) {
            Optional<GraphqlParser.InputObjectTypeDefinitionContext> original = getInputObject(typeDefinitionContext.inputObjectTypeDefinition().name().getText());
            if (original.isPresent()) {
                GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext = DOCUMENT_UTIL.graphqlToInputObjectTypeDefinition(InputObjectType.merge(original.get(), typeDefinitionContext.inputObjectTypeDefinition()).toString());
                graphQLInputObjectManager.register(inputObjectTypeDefinitionContext);
                graphQLInputValueManager.register(inputObjectTypeDefinitionContext);
            } else {
                graphQLInputObjectManager.register(typeDefinitionContext.inputObjectTypeDefinition());
                graphQLInputValueManager.register(typeDefinitionContext.inputObjectTypeDefinition());
            }
        }
    }

    @Override
    public boolean isScalar(String name) {
        return graphQLScalarManager.isScalar(name);
    }

    @Override
    public boolean isInnerScalar(String name) {
        return graphQLScalarManager.isInnerScalar(name);
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
    public boolean isContainerType(String objectTypeName) {
        return graphQLObjectManager.isContainerType(objectTypeName);
    }

    @Override
    public boolean isNotContainerType(String objectTypeName) {
        return graphQLObjectManager.isNotContainerType(objectTypeName);
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
    public boolean isGrpcField(String objectTypeName, String name) {
        return graphQLFieldManager.isGrpcField(objectTypeName, name);
    }

    @Override
    public boolean isNotGrpcField(String objectTypeName, String name) {
        return graphQLFieldManager.isNotGrpcField(objectTypeName, name);
    }

    @Override
    public boolean isFunctionField(String objectTypeName, String name) {
        return graphQLFieldManager.isFunctionField(objectTypeName, name);
    }

    @Override
    public boolean isNotFunctionField(String objectTypeName, String name) {
        return graphQLFieldManager.isNotFunctionField(objectTypeName, name);
    }

    @Override
    public boolean isConnectionField(String objectTypeName, String name) {
        return graphQLFieldManager.isConnectionField(objectTypeName, name);
    }

    @Override
    public boolean isNotConnectionField(String objectTypeName, String name) {
        return graphQLFieldManager.isNotConnectionField(objectTypeName, name);
    }

    @Override
    public boolean isInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return graphQLFieldManager.isInvokeField(fieldDefinitionContext);
    }

    @Override
    public boolean isNotInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return graphQLFieldManager.isNotInvokeField(fieldDefinitionContext);
    }

    @Override
    public boolean isGrpcField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return graphQLFieldManager.isGrpcField(fieldDefinitionContext);
    }

    @Override
    public boolean isNotGrpcField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return graphQLFieldManager.isNotGrpcField(fieldDefinitionContext);
    }

    @Override
    public boolean isFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return graphQLFieldManager.isFunctionField(fieldDefinitionContext);
    }

    @Override
    public boolean isNotFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return graphQLFieldManager.isNotFunctionField(fieldDefinitionContext);
    }

    @Override
    public boolean isConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return graphQLFieldManager.isConnectionField(fieldDefinitionContext);
    }

    @Override
    public boolean isNotConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return graphQLFieldManager.isNotConnectionField(fieldDefinitionContext);
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
    public Stream<GraphqlParser.FieldDefinitionContext> getFieldByDirective(String objectName, String directiveName) {
        return graphQLFieldManager.getFieldDefinitionByDirective(objectName, directiveName);
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
    public Stream<GraphqlParser.SelectionContext> fragmentUnzip(String typeName, GraphqlParser.SelectionContext selectionContext) {
        if (selectionContext.fragmentSpread() != null) {
            Optional<GraphqlParser.FragmentDefinitionContext> fragmentDefinitionContext = getObjectFragmentDefinition(typeName, selectionContext.fragmentSpread().fragmentName().getText());
            if (fragmentDefinitionContext.isPresent()) {
                return fragmentDefinitionContext.get().selectionSet().selection().stream();
            } else {
                throw new GraphQLErrors().add(FRAGMENT_NOT_EXIST.bind(selectionContext.fragmentSpread().fragmentName().getText()));
            }
        } else {
            return Stream.of(selectionContext);
        }
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
        if (argumentsContext == null) {
            return Optional.empty();
        }
        return argumentsContext.argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals(inputValueDefinitionContext.name().getText()))
                .filter(argumentContext -> !(argumentContext.valueWithVariable().NullValue() != null && inputValueDefinitionContext.type().nonNullType() != null))
                .findFirst();
    }

    @Override
    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getObjectFieldWithVariableFromInputValueDefinition(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (objectValueWithVariableContext == null) {
            return Optional.empty();
        }
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(inputValueDefinitionContext.name().getText()))
                .filter(objectFieldWithVariableContext -> !(objectFieldWithVariableContext.valueWithVariable().NullValue() != null && inputValueDefinitionContext.type().nonNullType() != null))
                .findFirst();
    }

    @Override
    public Optional<GraphqlParser.ObjectFieldContext> getObjectFieldFromInputValueDefinition(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (objectValueContext == null) {
            return Optional.empty();
        }
        return objectValueContext.objectField().stream()
                .filter(objectFieldContext -> objectFieldContext.name().getText().equals(inputValueDefinitionContext.name().getText()))
                .filter(objectFieldContext -> !(objectFieldContext.value().NullValue() != null && inputValueDefinitionContext.type().nonNullType() != null))
                .findFirst();
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
        if (argumentsContext == null) {
            return Optional.empty();
        }
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition
                .flatMap(fieldDefinitionContext ->
                        argumentsContext.argument().stream()
                                .filter(argumentContext -> argumentContext.valueWithVariable().NullValue() == null)
                                .filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText()))
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getIDObjectFieldWithVariable(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        if (objectValueWithVariableContext == null) {
            return Optional.empty();
        }
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition
                .flatMap(fieldDefinitionContext ->
                        objectValueWithVariableContext.objectFieldWithVariable().stream()
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().NullValue() == null)
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(fieldDefinitionContext.name().getText()))
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ObjectFieldContext> getIDObjectField(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
        if (objectValueContext == null) {
            return Optional.empty();
        }
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition
                .flatMap(fieldDefinitionContext ->
                        objectValueContext.objectField().stream()
                                .filter(objectFieldContext -> objectFieldContext.value().NullValue() == null)
                                .filter(objectFieldContext -> objectFieldContext.name().getText().equals(fieldDefinitionContext.name().getText()))
                                .findFirst()
                );
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
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
    }

    @Override
    public boolean fieldTypeIsList(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            return false;
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return false;
            } else if (typeContext.nonNullType().listType() != null) {
                return true;
            }
        } else if (typeContext.listType() != null) {
            return true;
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
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
