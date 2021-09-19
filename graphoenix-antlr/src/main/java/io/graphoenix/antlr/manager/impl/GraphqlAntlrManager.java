package io.graphoenix.antlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.common.utils.DocumentUtil;
import io.graphoenix.antlr.manager.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphqlAntlrManager {

    private final IGraphqlOperationManager graphqlOperationManager;

    private final IGraphqlSchemaManager graphqlSchemaManager;

    private final IGraphqlDirectiveManager graphqlDirectiveManager;

    private final IGraphqlObjectManager graphqlObjectManager;

    private final IGraphqlFieldManager graphqlFieldManager;

    private final IGraphqlInputObjectManager graphqlInputObjectManager;

    private final IGraphqlInputValueManager graphqlInputValueManager;

    private final IGraphqlEnumManager graphqlEnumManager;

    private final IGraphqlScalarManager graphqlScalarManager;

    public GraphqlAntlrManager(IGraphqlOperationManager graphqlOperationManager,
                               IGraphqlSchemaManager graphqlSchemaManager,
                               IGraphqlDirectiveManager graphqlDirectiveManager,
                               IGraphqlObjectManager graphqlObjectManager,
                               IGraphqlFieldManager graphqlFieldManager,
                               IGraphqlInputObjectManager graphqlInputObjectManager,
                               IGraphqlInputValueManager graphqlInputValueManager,
                               IGraphqlEnumManager graphqlEnumManager,
                               IGraphqlScalarManager graphqlScalarManager) {
        this.graphqlOperationManager = graphqlOperationManager;
        this.graphqlSchemaManager = graphqlSchemaManager;
        this.graphqlDirectiveManager = graphqlDirectiveManager;
        this.graphqlObjectManager = graphqlObjectManager;
        this.graphqlFieldManager = graphqlFieldManager;
        this.graphqlInputObjectManager = graphqlInputObjectManager;
        this.graphqlInputValueManager = graphqlInputValueManager;
        this.graphqlEnumManager = graphqlEnumManager;
        this.graphqlScalarManager = graphqlScalarManager;
    }

    public GraphqlAntlrManager() {
        this.graphqlOperationManager = new GraphqlOperationManager();
        this.graphqlSchemaManager = new GraphqlSchemaManager();
        this.graphqlDirectiveManager = new GraphqlDirectiveManager();
        this.graphqlObjectManager = new GraphqlObjectManager();
        this.graphqlFieldManager = new GraphqlFieldManager();
        this.graphqlInputObjectManager = new GraphqlInputObjectManager();
        this.graphqlInputValueManager = new GraphqlInputValueManager();
        this.graphqlEnumManager = new GraphqlEnumManager();
        this.graphqlScalarManager = new GraphqlScalarManager();
    }

    public GraphqlAntlrManager(GraphqlParser.DocumentContext documentContext) {
        this.graphqlOperationManager = new GraphqlOperationManager();
        this.graphqlSchemaManager = new GraphqlSchemaManager();
        this.graphqlDirectiveManager = new GraphqlDirectiveManager();
        this.graphqlObjectManager = new GraphqlObjectManager();
        this.graphqlFieldManager = new GraphqlFieldManager();
        this.graphqlInputObjectManager = new GraphqlInputObjectManager();
        this.graphqlInputValueManager = new GraphqlInputValueManager();
        this.graphqlEnumManager = new GraphqlEnumManager();
        this.graphqlScalarManager = new GraphqlScalarManager();
        this.registerDocument(documentContext);
    }

    public GraphqlAntlrManager(String graphql) {
        this.graphqlOperationManager = new GraphqlOperationManager();
        this.graphqlSchemaManager = new GraphqlSchemaManager();
        this.graphqlDirectiveManager = new GraphqlDirectiveManager();
        this.graphqlObjectManager = new GraphqlObjectManager();
        this.graphqlFieldManager = new GraphqlFieldManager();
        this.graphqlInputObjectManager = new GraphqlInputObjectManager();
        this.graphqlInputValueManager = new GraphqlInputValueManager();
        this.graphqlEnumManager = new GraphqlEnumManager();
        this.graphqlScalarManager = new GraphqlScalarManager();
        this.registerDocument(DocumentUtil.DOCUMENT_UTIL.graphqlToDocument(graphql));
    }

    public GraphqlAntlrManager(InputStream inputStream) throws IOException {
        this.graphqlOperationManager = new GraphqlOperationManager();
        this.graphqlSchemaManager = new GraphqlSchemaManager();
        this.graphqlDirectiveManager = new GraphqlDirectiveManager();
        this.graphqlObjectManager = new GraphqlObjectManager();
        this.graphqlFieldManager = new GraphqlFieldManager();
        this.graphqlInputObjectManager = new GraphqlInputObjectManager();
        this.graphqlInputValueManager = new GraphqlInputValueManager();
        this.graphqlEnumManager = new GraphqlEnumManager();
        this.graphqlScalarManager = new GraphqlScalarManager();
        this.registerDocument(DocumentUtil.DOCUMENT_UTIL.graphqlToDocument(inputStream));
    }

    public void registerDocument(String graphql) {
        DocumentUtil.DOCUMENT_UTIL.graphqlToDocument(graphql).definition().forEach(this::registerDefinition);
    }

    public void registerDocument(GraphqlParser.DocumentContext documentContext) {
        documentContext.definition().forEach(this::registerDefinition);
    }

    protected void registerDefinition(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.typeSystemDefinition() != null) {
            registerSystemDefinition(definitionContext.typeSystemDefinition());
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
        } else if (typeDefinitionContext.inputObjectTypeDefinition() != null) {
            graphqlInputObjectManager.register(typeDefinitionContext.inputObjectTypeDefinition());
            graphqlInputValueManager.register(typeDefinitionContext.inputObjectTypeDefinition());
        }
    }

    public boolean isScaLar(String name) {
        return graphqlScalarManager.isScalar(name);
    }

    public boolean isInnerScalar(String name) {
        return graphqlScalarManager.isInnerScalar(name);
    }

    public boolean isEnum(String name) {
        return graphqlEnumManager.isEnum(name);
    }

    public boolean isObject(String name) {
        return graphqlObjectManager.isObject(name);
    }

    public boolean isInputObject(String name) {
        return graphqlInputObjectManager.isInputObject(name);
    }

    public boolean isOperation(String name) {
        return graphqlOperationManager.isOperation(name);
    }

    public GraphqlParser.SchemaDefinitionContext getSchema() {
        return graphqlSchemaManager.getSchemaDefinitionContext();
    }

    public Optional<GraphqlParser.DirectiveDefinitionContext> getDirective(String name) {
        return graphqlDirectiveManager.getDirectiveDefinition(name);
    }

    public Optional<GraphqlParser.ScalarTypeDefinitionContext> getScaLar(String name) {
        return graphqlScalarManager.getScalarTypeDefinition(name);
    }

    public Optional<GraphqlParser.EnumTypeDefinitionContext> getEnum(String name) {
        return graphqlEnumManager.getEnumTypeDefinition(name);
    }

    public Optional<GraphqlParser.ObjectTypeDefinitionContext> getObject(String name) {
        return graphqlObjectManager.getObjectTypeDefinition(name);
    }

    public Optional<GraphqlParser.InputObjectTypeDefinitionContext> getInputObject(String name) {
        return graphqlInputObjectManager.getInputObjectTypeDefinition(name);
    }

    public Stream<GraphqlParser.DirectiveDefinitionContext> getDirectives() {
        return graphqlDirectiveManager.getDirectiveDefinitions();
    }

    public Stream<GraphqlParser.EnumTypeDefinitionContext> getEnums() {
        return graphqlEnumManager.getEnumTypeDefinitions();
    }

    public Stream<GraphqlParser.ObjectTypeDefinitionContext> getObjects() {
        return graphqlObjectManager.getObjectTypeDefinitions();
    }

    public Stream<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjects() {
        return graphqlInputObjectManager.getInputObjectTypeDefinitions();
    }

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getOperation(String name) {
        return graphqlOperationManager.getOperationTypeDefinition(name);
    }

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getQueryOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().QUERY() != null).findFirst();
    }

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getMutationOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().MUTATION() != null).findFirst();
    }

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getSubscriptionOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions()
                .filter(operationTypeDefinition -> operationTypeDefinition.operationType().SUBSCRIPTION() != null).findFirst();
    }

    public Optional<String> getObjectFieldTypeName(String typeName, String fieldName) {
        return graphqlFieldManager.getFieldDefinition(typeName, fieldName).map(fieldDefinitionContext -> getFieldTypeName(fieldDefinitionContext.type()));
    }

    public Optional<GraphqlParser.FieldDefinitionContext> getObjectFieldDefinitionContext(String typeName, String fieldName) {
        return graphqlFieldManager.getFieldDefinition(typeName, fieldName);
    }

    public Optional<String> getQueryOperationTypeName() {
        Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = getQueryOperationTypeDefinition();
        return queryOperationTypeDefinition.map(operationTypeDefinitionContext -> operationTypeDefinitionContext.typeName().name().getText());
    }

    public boolean isQueryOperationType(String typeName) {
        Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = getQueryOperationTypeDefinition();
        return queryOperationTypeDefinition.isPresent() && queryOperationTypeDefinition.get().typeName().name().getText().equals(typeName);
    }

    public Optional<String> getMutationOperationTypeName() {
        Optional<GraphqlParser.OperationTypeDefinitionContext> mutationOperationTypeDefinition = getMutationOperationTypeDefinition();
        return mutationOperationTypeDefinition.map(operationTypeDefinitionContext -> operationTypeDefinitionContext.typeName().name().getText());
    }

    public boolean isMutationOperationType(String typeName) {
        Optional<GraphqlParser.OperationTypeDefinitionContext> mutationOperationTypeDefinition = getMutationOperationTypeDefinition();
        return mutationOperationTypeDefinition.isPresent() && mutationOperationTypeDefinition.get().typeName().name().getText().equals(typeName);
    }

    public Optional<String> getSubscriptionOperationTypeName() {
        Optional<GraphqlParser.OperationTypeDefinitionContext> subscriptionOperationTypeDefinition = getSubscriptionOperationTypeDefinition();
        return subscriptionOperationTypeDefinition.map(operationTypeDefinitionContext -> operationTypeDefinitionContext.typeName().name().getText());
    }

    public boolean isSubscriptionOperationType(String typeName) {
        Optional<GraphqlParser.OperationTypeDefinitionContext> subscriptionOperationTypeDefinition = getSubscriptionOperationTypeDefinition();
        return subscriptionOperationTypeDefinition.isPresent() && subscriptionOperationTypeDefinition.get().typeName().name().getText().equals(typeName);
    }

    public Optional<GraphqlParser.FieldDefinitionContext> getObjectTypeIDFieldDefinition(String objectTypeName) {
        return graphqlFieldManager.getFieldDefinitions(objectTypeName)
                .filter(fieldDefinitionContext -> !fieldTypeIsList(fieldDefinitionContext.type()))
                .filter(fieldDefinitionContext -> getFieldTypeName(fieldDefinitionContext.type()).equals("ID")).findFirst();
    }

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

    public Optional<GraphqlParser.ArgumentContext> getMapWithTypeArgument(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (fieldDefinitionContext.directives() == null) {
            return Optional.empty();
        }
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals("map")).findFirst()
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream()
                        .filter(argumentContext -> argumentContext.name().getText().equals("with")).findAny());
    }

    public Optional<String> getMapWithTypeName(GraphqlParser.ArgumentContext argumentContext) {
        return argumentContext.valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("type"))
                .map(fieldWithVariableContext -> fieldWithVariableContext.valueWithVariable().StringValue().getText())
                .findAny();
    }

    public Optional<String> getMapWithTypeFromFieldName(GraphqlParser.ArgumentContext argumentContext) {
        return argumentContext.valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("from"))
                .map(fieldWithVariableContext -> fieldWithVariableContext.valueWithVariable().StringValue().getText())
                .findAny();
    }

    public Optional<String> getMapWithTypeToFieldName(GraphqlParser.ArgumentContext argumentContext) {
        return argumentContext.valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("to"))
                .map(fieldWithVariableContext -> fieldWithVariableContext.valueWithVariable().StringValue().getText())
                .findAny();
    }

    public Optional<String> getObjectTypeIDFieldName(String objectTypeName) {
        return graphqlFieldManager.getFieldDefinitions(objectTypeName)
                .filter(fieldDefinitionContext -> !fieldTypeIsList(fieldDefinitionContext.type()))
                .filter(fieldDefinitionContext -> getFieldTypeName(fieldDefinitionContext.type()).equals("ID")).findFirst()
                .map(fieldDefinitionContext -> fieldDefinitionContext.name().getText());
    }

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromArgumentsDefinitionContext(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return argumentsDefinitionContext.inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(argumentContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldWithVariableContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.ArgumentContext> getArgumentFromInputValueDefinition(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getObjectFieldWithVariableFromInputValueDefinition(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream().filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.ObjectFieldContext> getObjectFieldFromInputValueDefinition(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return objectValueContext.objectField().stream().filter(objectFieldContext -> objectFieldContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinitionFromInputValueDefinition(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return graphqlFieldManager.getFieldDefinitions(getFieldTypeName(typeContext))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.ArgumentContext> getIDArgument(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition.flatMap(fieldDefinitionContext -> argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText())).findFirst());
    }

    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getIDObjectFieldWithVariable(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition.flatMap(fieldDefinitionContext -> objectValueWithVariableContext.objectFieldWithVariable().stream().filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText())).findFirst());
    }

    public Optional<GraphqlParser.ObjectFieldContext> getIDObjectField(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition.flatMap(fieldDefinitionContext -> objectValueContext.objectField().stream().filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText())).findFirst());
    }

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

    public boolean fieldTypeIsList(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            return false;
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return false;
            } else return typeContext.nonNullType().listType() != null;
        } else return typeContext.listType() != null;
    }
}
