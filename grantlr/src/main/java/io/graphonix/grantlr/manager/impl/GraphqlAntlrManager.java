package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.*;

import java.util.Map;
import java.util.Optional;

public class GraphqlAntlrManager {

    private final IGraphqlOperationManager graphqlOperationManager;

    private final IGraphqlObjectManager graphqlObjectManager;

    private final IGraphqlFieldManager graphqlFieldManager;

    private final IGraphqlInputObjectManager graphqlInputObjectManager;

    private final IGraphqlInputValueManager graphqlInputValueManager;

    private final IGraphqlEnumManager graphqlEnumManager;

    private final IGraphqlScalarManager graphqlScalarManager;

    public GraphqlAntlrManager(IGraphqlOperationManager graphqlOperationManager,
                               IGraphqlObjectManager graphqlObjectManager,
                               IGraphqlFieldManager graphqlFieldManager,
                               IGraphqlInputObjectManager graphqlInputObjectManager,
                               IGraphqlInputValueManager graphqlInputValueManager,
                               IGraphqlEnumManager graphqlEnumManager,
                               IGraphqlScalarManager graphqlScalarManager) {
        this.graphqlOperationManager = graphqlOperationManager;
        this.graphqlObjectManager = graphqlObjectManager;
        this.graphqlFieldManager = graphqlFieldManager;
        this.graphqlInputObjectManager = graphqlInputObjectManager;
        this.graphqlInputValueManager = graphqlInputValueManager;
        this.graphqlEnumManager = graphqlEnumManager;
        this.graphqlScalarManager = graphqlScalarManager;
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
        } else if (typeSystemDefinitionContext.typeDefinition() != null) {
            registerTypeDefinition(typeSystemDefinitionContext.typeDefinition());
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

    public boolean exist(String name) {
        return isScaLar(name) || isEnum(name) || isObject(name) || isInputObject(name);
    }

    public boolean isScaLar(String name) {
        return graphqlScalarManager.getScalarTypeDefinition(name) != null;
    }

    public boolean isEnum(String name) {
        return graphqlEnumManager.getEnumTypeDefinition(name) != null;
    }

    public boolean isObject(String name) {
        return graphqlObjectManager.getObjectTypeDefinition(name) != null;
    }

    public boolean isInputObject(String name) {
        return graphqlInputObjectManager.getInputObjectTypeDefinition(name) != null;
    }

    public boolean isOperationType(String name) {
        return graphqlOperationManager.getOperationTypeDefinition(name) != null;
    }

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getQueryOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions().entrySet().stream()
                .filter(operationTypeDefinitionContextEntry -> operationTypeDefinitionContextEntry.getValue().operationType().QUERY() != null).findFirst().map(Map.Entry::getValue);
    }

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getMutationOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions().entrySet().stream()
                .filter(operationTypeDefinitionContextEntry -> operationTypeDefinitionContextEntry.getValue().operationType().MUTATION() != null).findFirst().map(Map.Entry::getValue);
    }

    public Optional<GraphqlParser.OperationTypeDefinitionContext> getSubscriptionOperationTypeDefinition() {
        return graphqlOperationManager.getOperationTypeDefinitions().entrySet().stream()
                .filter(operationTypeDefinitionContextEntry -> operationTypeDefinitionContextEntry.getValue().operationType().SUBSCRIPTION() != null).findFirst().map(Map.Entry::getValue);
    }

    public Optional<GraphqlParser.FieldDefinitionContext> getObjectTypeIDFieldDefinition(String objectTypeName) {
        return graphqlFieldManager.getFieldDefinitions(objectTypeName).entrySet().stream()
                .filter(entry -> !fieldTypeIsList(entry.getValue().type()))
                .filter(entry -> getFieldTypeName(entry.getValue().type()).equals("ID")).findFirst()
                .map(Map.Entry::getValue);
    }

    public Optional<GraphqlParser.FieldDefinitionContext> getObjectTypeRelationFieldDefinition(String sourceObjectTypeName, String targetObjectTypeName) {
        return graphqlFieldManager.getFieldDefinitions(sourceObjectTypeName).entrySet().stream()
                .filter(entry -> !fieldTypeIsList(entry.getValue().type()))
                .filter(entry -> getFieldTypeName(entry.getValue().type()).equals(targetObjectTypeName)).findFirst()
                .map(Map.Entry::getValue);
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
        return graphqlFieldManager.getFieldDefinitions(getFieldTypeName(typeContext)).entrySet().stream()
                .filter(entry -> entry.getValue().name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst()
                .map(Map.Entry::getValue);
    }

    protected Optional<GraphqlParser.ArgumentContext> getIDArgument(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition.flatMap(fieldDefinitionContext -> argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText())).findFirst());
    }

    protected Optional<GraphqlParser.ObjectFieldWithVariableContext> getIDObjectFieldWithVariable(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = getObjectTypeIDFieldDefinition(getFieldTypeName(typeContext));
        return idFieldDefinition.flatMap(fieldDefinitionContext -> objectValueWithVariableContext.objectFieldWithVariable().stream().filter(argumentContext -> argumentContext.name().getText().equals(fieldDefinitionContext.name().getText())).findFirst());
    }

    protected Optional<GraphqlParser.ObjectFieldContext> getIDObjectField(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
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
