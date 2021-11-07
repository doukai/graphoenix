package io.graphoenix.common.manager;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

import static io.graphoenix.common.utils.DocumentUtil.DOCUMENT_UTIL;

@AutoFactory
public class GraphqlDocumentManager implements IGraphqlDocumentManager {

    private final IGraphqlOperationManager graphqlOperationManager;

    private final IGraphqlSchemaManager graphqlSchemaManager;

    private final IGraphqlDirectiveManager graphqlDirectiveManager;

    private final IGraphqlObjectManager graphqlObjectManager;

    private final IGraphqlInterfaceManager graphqlInterfaceManager;

    private final IGraphqlUnionManager graphqlUnionManager;

    private final IGraphqlFieldManager graphqlFieldManager;

    private final IGraphqlInputObjectManager graphqlInputObjectManager;

    private final IGraphqlInputValueManager graphqlInputValueManager;

    private final IGraphqlEnumManager graphqlEnumManager;

    private final IGraphqlScalarManager graphqlScalarManager;

    private final IGraphqlFragmentManager graphqlFragmentManager;

    public GraphqlDocumentManager(@Provided IGraphqlOperationManager graphqlOperationManager,
                                  @Provided IGraphqlSchemaManager graphqlSchemaManager,
                                  @Provided IGraphqlDirectiveManager graphqlDirectiveManager,
                                  @Provided IGraphqlObjectManager graphqlObjectManager,
                                  @Provided IGraphqlInterfaceManager graphqlInterfaceManager,
                                  @Provided IGraphqlUnionManager graphqlUnionManager,
                                  @Provided IGraphqlFieldManager graphqlFieldManager,
                                  @Provided IGraphqlInputObjectManager graphqlInputObjectManager,
                                  @Provided IGraphqlInputValueManager graphqlInputValueManager,
                                  @Provided IGraphqlEnumManager graphqlEnumManager,
                                  @Provided IGraphqlScalarManager graphqlScalarManager,
                                  @Provided IGraphqlFragmentManager graphqlFragmentManager) {
        this.graphqlOperationManager = graphqlOperationManager;
        this.graphqlSchemaManager = graphqlSchemaManager;
        this.graphqlDirectiveManager = graphqlDirectiveManager;
        this.graphqlObjectManager = graphqlObjectManager;
        this.graphqlInterfaceManager = graphqlInterfaceManager;
        this.graphqlUnionManager = graphqlUnionManager;
        this.graphqlFieldManager = graphqlFieldManager;
        this.graphqlInputObjectManager = graphqlInputObjectManager;
        this.graphqlInputValueManager = graphqlInputValueManager;
        this.graphqlEnumManager = graphqlEnumManager;
        this.graphqlScalarManager = graphqlScalarManager;
        this.graphqlFragmentManager = graphqlFragmentManager;
    }

    @Override
    public void registerDocument(String graphql) {
        DOCUMENT_UTIL.graphqlToDocument(graphql).definition().forEach(this::registerDefinition);
    }

    @Override
    public void registerDocument(InputStream inputStream) throws IOException {
        DOCUMENT_UTIL.graphqlToDocument(inputStream).definition().forEach(this::registerDefinition);
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
    public GraphqlParser.OperationTypeContext getOperationType(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream()
                .filter(definitionContext -> definitionContext.operationDefinition() != null)
                .findFirst()
                .map(definitionContext -> definitionContext.operationDefinition().operationType())
                .orElse(null);
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
            graphqlFragmentManager.register(definitionContext.fragmentDefinition());
        }
    }

    protected void registerSystemDefinition(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.schemaDefinition() != null) {
            typeSystemDefinitionContext.schemaDefinition().operationTypeDefinition().forEach(this::registerOperationType);
            graphqlSchemaManager.register(typeSystemDefinitionContext.schemaDefinition());
        } else if (typeSystemDefinitionContext.typeDefinition() != null) {
            registerTypeDefinition(typeSystemDefinitionContext.typeDefinition());
        } else if (typeSystemDefinitionContext.directiveDefinition() != null) {
            graphqlDirectiveManager.register(typeSystemDefinitionContext.directiveDefinition());
        }
    }

    protected void registerOperationType(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        graphqlOperationManager.register(operationTypeDefinitionContext);
    }

    protected void registerTypeDefinition(GraphqlParser.TypeDefinitionContext typeDefinitionContext) {

        if (typeDefinitionContext.scalarTypeDefinition() != null) {
            graphqlScalarManager.register(typeDefinitionContext.scalarTypeDefinition());
        } else if (typeDefinitionContext.enumTypeDefinition() != null) {
            graphqlEnumManager.register(typeDefinitionContext.enumTypeDefinition());
        } else if (typeDefinitionContext.objectTypeDefinition() != null) {
            graphqlObjectManager.register(typeDefinitionContext.objectTypeDefinition());
            graphqlFieldManager.register(typeDefinitionContext.objectTypeDefinition());
        } else if (typeDefinitionContext.interfaceTypeDefinition() != null) {
            graphqlInterfaceManager.register(typeDefinitionContext.interfaceTypeDefinition());
            graphqlFieldManager.register(typeDefinitionContext.interfaceTypeDefinition());
        } else if (typeDefinitionContext.unionTypeDefinition() != null) {
            graphqlUnionManager.register(typeDefinitionContext.unionTypeDefinition());
        } else if (typeDefinitionContext.inputObjectTypeDefinition() != null) {
            graphqlInputObjectManager.register(typeDefinitionContext.inputObjectTypeDefinition());
            graphqlInputValueManager.register(typeDefinitionContext.inputObjectTypeDefinition());
        }
    }

    @Override
    public boolean isScaLar(String name) {
        return graphqlScalarManager.isScalar(name);
    }

    @Override
    public boolean isInnerScalar(String name) {
        return graphqlScalarManager.isInnerScalar(name);
    }

    @Override
    public boolean isEnum(String name) {
        return graphqlEnumManager.isEnum(name);
    }

    @Override
    public boolean isObject(String name) {
        return graphqlObjectManager.isObject(name);
    }

    @Override
    public boolean isInterface(String name) {
        return graphqlInterfaceManager.isInterface(name);
    }

    @Override
    public boolean isUnion(String name) {
        return graphqlUnionManager.isUnion(name);
    }

    @Override
    public boolean isInputObject(String name) {
        return graphqlInputObjectManager.isInputObject(name);
    }

    @Override
    public boolean isOperation(String name) {
        return graphqlOperationManager.isOperation(name);
    }

    @Override
    public GraphqlParser.SchemaDefinitionContext getSchema() {
        return graphqlSchemaManager.getSchemaDefinitionContext();
    }

    @Override
    public Optional<GraphqlParser.DirectiveDefinitionContext> getDirective(String name) {
        return graphqlDirectiveManager.getDirectiveDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.ScalarTypeDefinitionContext> getScaLar(String name) {
        return graphqlScalarManager.getScalarTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.EnumTypeDefinitionContext> getEnum(String name) {
        return graphqlEnumManager.getEnumTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.ObjectTypeDefinitionContext> getObject(String name) {
        return graphqlObjectManager.getObjectTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.InterfaceTypeDefinitionContext> getInterface(String name) {
        return graphqlInterfaceManager.getInterfaceTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.UnionTypeDefinitionContext> getUnion(String name) {
        return graphqlUnionManager.getUnionTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.InputObjectTypeDefinitionContext> getInputObject(String name) {
        return graphqlInputObjectManager.getInputObjectTypeDefinition(name);
    }

    @Override
    public Stream<GraphqlParser.DirectiveDefinitionContext> getDirectives() {
        return graphqlDirectiveManager.getDirectiveDefinitions();
    }

    @Override
    public Stream<GraphqlParser.EnumTypeDefinitionContext> getEnums() {
        return graphqlEnumManager.getEnumTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.ObjectTypeDefinitionContext> getObjects() {
        return graphqlObjectManager.getObjectTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaces() {
        return graphqlInterfaceManager.getInterfaceTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.UnionTypeDefinitionContext> getUnions() {
        return graphqlUnionManager.getUnionTypeDefinitions();
    }

    @Override
    public Stream<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjects() {
        return graphqlInputObjectManager.getInputObjectTypeDefinitions();
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getOperation(String name) {
        return graphqlOperationManager.getOperationTypeDefinition(name);
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getQueryOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().QUERY() != null).findFirst();
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getMutationOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().MUTATION() != null).findFirst();
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getSubscriptionOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().SUBSCRIPTION() != null).findFirst();
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getQueryOperationFieldDefinitionContext(String typeName, boolean list) {
        return getObjects().filter(objectTypeDefinitionContext -> isQueryOperationType(objectTypeDefinitionContext.name().getText())).findFirst()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(fieldDefinitionContext -> getFieldTypeName(fieldDefinitionContext.type()).equals(typeName))
                        .filter(fieldDefinitionContext -> fieldTypeIsList(fieldDefinitionContext.type()) == list)
                        .findFirst());
    }

    @Override
    public Optional<String> getObjectFieldTypeName(String typeName, String fieldName) {
        return graphqlFieldManager.getFieldDefinition(typeName, fieldName).map(fieldDefinitionContext -> getFieldTypeName(fieldDefinitionContext.type()));
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getObjectFieldDefinitionContext(String typeName, String fieldName) {
        return graphqlFieldManager.getFieldDefinition(typeName, fieldName);
    }

    @Override
    public Optional<GraphqlParser.FragmentDefinitionContext> getObjectFragmentDefinitionContext(String typeName, String fragmentName) {
        return graphqlFragmentManager.getFragmentDefinition(typeName, fragmentName);
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
        return graphqlFieldManager.getFieldDefinitions(objectTypeName)
                .filter(fieldDefinitionContext -> !fieldTypeIsList(fieldDefinitionContext.type()))
                .filter(fieldDefinitionContext -> getFieldTypeName(fieldDefinitionContext.type()).equals("ID")).findFirst();
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getMapFromFieldDefinition(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (fieldDefinitionContext.directives() == null) {
            return Optional.empty();
        }
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals("map")).findFirst()
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("from")).findFirst())
                .map(argumentContext -> argumentContext.valueWithVariable().StringValue().getText())
                .flatMap(fromFieldName -> getObjectFieldDefinitionContext(typeName, fromFieldName.substring(1, fromFieldName.length() - 1)));
    }

    @Override
    public Optional<GraphqlParser.ValueWithVariableContext> getMapFromValueWithVariableFromArguments(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                                     GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                     GraphqlParser.ArgumentsContext parentArgumentsContext) {

        return getMapFromFieldDefinition(getFieldTypeName(parentFieldDefinitionContext.type()), fieldDefinitionContext)
                .flatMap(fromFieldDefinitionContext ->
                        parentArgumentsContext.argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals(fromFieldDefinitionContext.name().getText()))
                                .map(GraphqlParser.ArgumentContext::valueWithVariable)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ValueWithVariableContext> getMapFromValueWithVariableFromObjectFieldWithVariable(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                                   GraphqlParser.ObjectValueWithVariableContext parentObjectValueWithVariableContext) {
        return getMapFromFieldDefinition(getFieldTypeName(parentFieldDefinitionContext.type()), fieldDefinitionContext)
                .flatMap(fromFieldDefinitionContext ->
                        parentObjectValueWithVariableContext.objectFieldWithVariable().stream()
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(fromFieldDefinitionContext.name().getText()))
                                .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ValueContext> getMapFromValueFromObjectField(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                               GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                               GraphqlParser.ObjectValueContext parentObjectValueContext) {

        return getMapFromFieldDefinition(getFieldTypeName(parentFieldDefinitionContext.type()), fieldDefinitionContext)
                .flatMap(fromFieldDefinitionContext ->
                        parentObjectValueContext.objectField().stream()
                                .filter(objectFieldContext -> objectFieldContext.name().getText().equals(fromFieldDefinitionContext.name().getText()))
                                .map(GraphqlParser.ObjectFieldContext::value)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getMapToFieldDefinition(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (fieldDefinitionContext.directives() == null) {
            return Optional.empty();
        }
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals("map")).findFirst()
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("to")).findFirst())
                .map(argumentContext -> argumentContext.valueWithVariable().StringValue().getText())
                .flatMap(toFieldName -> getObjectFieldDefinitionContext(getFieldTypeName(fieldDefinitionContext.type()), toFieldName.substring(1, toFieldName.length() - 1)));
    }

    @Override
    public Optional<GraphqlParser.ValueWithVariableContext> getMapToValueWithVariableFromObjectFieldWithVariable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                                 GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        return getMapToFieldDefinition(fieldDefinitionContext)
                .flatMap(fromFieldDefinitionContext ->
                        objectValueWithVariableContext.objectFieldWithVariable().stream()
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(fromFieldDefinitionContext.name().getText()))
                                .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ValueContext> getMapToValueFromObjectField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                             GraphqlParser.ObjectValueContext objectValueContext) {

        return getMapToFieldDefinition(fieldDefinitionContext)
                .flatMap(fromFieldDefinitionContext ->
                        objectValueContext.objectField().stream()
                                .filter(objectFieldContext -> objectFieldContext.name().getText().equals(fromFieldDefinitionContext.name().getText()))
                                .map(GraphqlParser.ObjectFieldContext::value)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ArgumentContext> getMapWithTypeArgument(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (fieldDefinitionContext.directives() == null) {
            return Optional.empty();
        }
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals("map")).findFirst()
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream()
                        .filter(argumentContext -> argumentContext.name().getText().equals("with")).findAny());
    }

    @Override
    public Optional<String> getMapWithTypeName(GraphqlParser.ArgumentContext argumentContext) {
        return argumentContext.valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("type"))
                .map(fieldWithVariableContext -> fieldWithVariableContext.valueWithVariable().StringValue().getText())
                .map(string -> string.substring(1, string.length() - 1))
                .findAny();
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getMapWithTypeFromFieldDefinition(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return getMapWithTypeArgument(fieldDefinitionContext).flatMap(
                argumentContext -> argumentContext.valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                        .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("from"))
                        .map(fieldWithVariableContext -> fieldWithVariableContext.valueWithVariable().StringValue().getText())
                        .map(string -> string.substring(1, string.length() - 1))
        );
    }

    @Override
    public Optional<String> getMapWithTypeToFieldName(GraphqlParser.ArgumentContext argumentContext) {
        return argumentContext.valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("to"))
                .map(fieldWithVariableContext -> fieldWithVariableContext.valueWithVariable().StringValue().getText())
                .map(string -> string.substring(1, string.length() - 1))
                .findAny();
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getMapWithTypeFromFieldDefinition(GraphqlParser.ArgumentContext argumentContext) {
        Optional<String> typeName = getMapWithTypeName(argumentContext);
        Optional<String> fromFieldName = getMapWithTypeFromFieldName(argumentContext);
        if (typeName.isPresent() && fromFieldName.isPresent()) {
            return getObjectFieldDefinitionContext(typeName.get(), fromFieldName.get());
        }
        return Optional.empty();
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getMapWithTypeToFieldDefinition(GraphqlParser.ArgumentContext argumentContext) {
        Optional<String> typeName = getMapWithTypeName(argumentContext);
        Optional<String> toFieldName = getMapWithTypeToFieldName(argumentContext);
        if (typeName.isPresent() && toFieldName.isPresent()) {
            return getObjectFieldDefinitionContext(typeName.get(), toFieldName.get());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getObjectTypeIDFieldName(String objectTypeName) {
        return graphqlFieldManager.getFieldDefinitions(objectTypeName)
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
        return graphqlFieldManager.getFieldDefinitions(getFieldTypeName(typeContext))
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
    public Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinitionFromOperationTypeDefinitionContext(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext, GraphqlParser.SelectionContext selectionContext) {
        return graphqlFieldManager.getFieldDefinitions(operationTypeDefinitionContext.typeName().name().getText())
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(selectionContext.field().name().getText())).findFirst();
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
    public boolean fieldTypeIsNonNull(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            return false;
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return true;
            } else if (typeContext.nonNullType().listType() != null) {
                return fieldTypeIsNonNull(typeContext.nonNullType().listType().type());
            }
        } else if (typeContext.listType() != null) {
            return fieldTypeIsNonNull(typeContext.listType().type());
        }
        return false;
    }

    @Override
    public boolean fieldTypeIsNonNullList(GraphqlParser.TypeContext typeContext) {
        return typeContext.nonNullType() != null && typeContext.nonNullType().listType() != null;
    }
}
