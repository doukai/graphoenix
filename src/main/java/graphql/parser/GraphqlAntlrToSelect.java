package graphql.parser;

import com.google.common.base.CharMatcher;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

public class GraphqlAntlrToSelect {

    private final GraphqlAntlrRegister register;

    public GraphqlAntlrToSelect(GraphqlAntlrRegister register) {
        this.register = register;
    }

    public List<Select> createSelects(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream().map(this::createSelect).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    protected Optional<Select> createSelect(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() == null) {
            return Optional.empty();
        }
        return operationDefinitionToSelect(definitionContext.operationDefinition());
    }

    protected Optional<Select> operationDefinitionToSelect(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().getText().equals("query")) {
            Select select = new Select();
            PlainSelect body = new PlainSelect();
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
            selectExpressionItem.setExpression(objectFieldSelectionToJsonObjectFunction(null, operationDefinitionContext.selectionSet().selection()));
            body.setSelectItems(Collections.singletonList(selectExpressionItem));

            Table table = new Table("dual");
            body.setFromItem(table);
            select.setSelectBody(body);

            if (operationDefinitionContext.name() != null) {
                //TODO
            }
            if (operationDefinitionContext.variableDefinitions() != null) {
                //TODO
            }
            if (operationDefinitionContext.directives() != null) {
                //TODO
            }
            return Optional.of(select);
        }
        return Optional.empty();
    }

    protected Expression selectionToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        String typeName = typeContext == null ? register.getQueryTypeName() : register.getFieldTypeName(typeContext);
        String filedTypeName = register.getObjectFieldTypeName(typeName, selectionContext.field().name().getText());
        if (register.isObject(filedTypeName)) {
            return objectFieldToSubSelect(typeName, filedTypeName, typeContext, selectionContext);
        } else if (register.isScaLar(filedTypeName) || register.isInnerScalar(filedTypeName)) {
            if (typeContext != null) {
                return scaLarFieldToColumn(typeContext, selectionContext);
            }
        }
        return null;
    }

    protected Column scaLarFieldToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {

        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(selectionContext.field().name().getText()));
    }

    protected SubSelect objectFieldToSubSelect(String typeName, String filedTypeName, GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {

        Optional<GraphqlParser.TypeContext> fieldTypeContext = register.getObjectFieldTypeContext(typeName, selectionContext.field().name().getText());
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText());
        if (fieldTypeContext.isPresent()) {
            SubSelect subSelect = new SubSelect();
            PlainSelect body = new PlainSelect();
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();

            selectExpressionItem.setExpression(selectionToJsonFunction(fieldTypeContext.get(), selectionContext));

            body.setSelectItems(Collections.singletonList(selectExpressionItem));
            subSelect.setSelectBody(body);

            Table subTable = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext.get())));
            body.setFromItem(subTable);

            if (typeContext != null) {
                String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
                Table table = new Table(tableName);
                EqualsTo equalsTo = new EqualsTo();

                if (register.fieldTypeIsList(fieldTypeContext.get())) {
                    equalsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(filedTypeName, typeName))));
                    equalsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(typeName))));
                } else {
                    equalsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(filedTypeName))));
                    equalsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(selectionContext.field().name().getText())));
                }
                body.setWhere(equalsTo);
            } else {
                if (fieldDefinitionContext.isPresent() && selectionContext.field().arguments() != null) {
                    body.setWhere(argumentsToMultipleExpression(fieldTypeContext.get(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments()));
                }
            }
            return subSelect;
        }
        return null;
    }

    protected Function selectionToJsonFunction(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        if (register.fieldTypeIsList(typeContext)) {
            return listFieldSelectionToJsonArrayFunction(typeContext, selectionContext.field().selectionSet().selection());
        } else {
            return objectFieldSelectionToJsonObjectFunction(typeContext, selectionContext.field().selectionSet().selection());
        }
    }

    protected Function listFieldSelectionToJsonArrayFunction(GraphqlParser.TypeContext typeContext, List<GraphqlParser.SelectionContext> selectionContexts) {
        Function function = new Function();
        function.setName("JSON_ARRAYAGG");
        function.setParameters(new ExpressionList(objectFieldSelectionToJsonObjectFunction(typeContext, selectionContexts)));

        return function;
    }

    protected Function objectFieldSelectionToJsonObjectFunction(GraphqlParser.TypeContext typeContext, List<GraphqlParser.SelectionContext> selectionContexts) {
        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(new ExpressionList(selectionContexts.stream()
                .map(selectionContext -> new ExpressionList(new StringValue(selectionContext.field().name().getText()), selectionToExpression(typeContext, selectionContext)))
                .map(ExpressionList::getExpressions).flatMap(Collection::stream).collect(Collectors.toList())));

        return function;
    }

    protected Expression argumentsToMultipleExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        if (hasOrConditional(argumentsContext, argumentsDefinitionContext)) {
            return new MultiOrExpression(argumentsToExpressionList(fieldTypeContext, argumentsDefinitionContext, argumentsContext));
        }
        return new MultiAndExpression(argumentsToExpressionList(fieldTypeContext, argumentsDefinitionContext, argumentsContext));
    }

    protected Expression objectValueWithVariableToMultipleExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));
        if (hasOrConditional(objectValueWithVariableContext, typeDefinitionContext.inputObjectTypeDefinition())) {
            return new MultiOrExpression(objectValueWithVariableToExpressionList(fieldTypeContext, typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions(), objectValueWithVariableContext));
        }
        return new MultiAndExpression(objectValueWithVariableToExpressionList(fieldTypeContext, typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions(), objectValueWithVariableContext));
    }

    protected Expression objectValueToMultipleExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));
        if (hasOrConditional(objectValueContext, typeDefinitionContext.inputObjectTypeDefinition())) {
            return new MultiOrExpression(objectValueToExpressionList(fieldTypeContext, typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions(), objectValueContext));
        }
        return new MultiAndExpression(objectValueToExpressionList(fieldTypeContext, typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions(), objectValueContext));
    }

    protected List<Expression> argumentsToExpressionList(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        List<Expression> expressionList = argumentsDefinitionContext.inputValueDefinition().stream().filter(this::isNotConditional).map(inputValueDefinitionContext -> argumentsToExpression(fieldTypeContext, inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        Optional<List<Expression>> conditionalExpressionList = multiConditionalArgumentsToExpressionList(fieldTypeContext, argumentsDefinitionContext, argumentsContext);
        conditionalExpressionList.ifPresent(expressionList::addAll);
        return expressionList;
    }

    protected List<Expression> objectValueWithVariableToExpressionList(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        List<Expression> expressionList = inputObjectValueDefinitionsContext.inputValueDefinition().stream().filter(this::isNotConditional).map(inputValueDefinitionContext -> objectValueWithVariableToExpression(fieldTypeContext, inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        Optional<List<Expression>> conditionalExpressionList = multiConditionalObjectValueWithVariableToExpressionList(fieldTypeContext, inputObjectValueDefinitionsContext, objectValueWithVariableContext);
        conditionalExpressionList.ifPresent(expressionList::addAll);
        return expressionList;
    }

    protected List<Expression> objectValueToExpressionList(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext, GraphqlParser.ObjectValueContext objectValueContext) {
        List<Expression> expressionList = inputObjectValueDefinitionsContext.inputValueDefinition().stream().filter(this::isNotConditional).map(inputValueDefinitionContext -> objectValueToExpression(fieldTypeContext, inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        Optional<List<Expression>> conditionalExpressionList = multiConditionalObjectValueToExpression(fieldTypeContext, inputObjectValueDefinitionsContext, objectValueContext);
        conditionalExpressionList.ifPresent(expressionList::addAll);
        return expressionList;
    }

    protected Optional<Expression> argumentsToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return argumentToExpression(fieldTypeContext, inputValueDefinitionContext, argumentContext.get());
        } else {
            return defaultValueToExpression(fieldTypeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectValueWithVariableToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return objectFieldWithVariableToExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldWithVariableContext.get());
        } else {
            return defaultValueToExpression(fieldTypeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectValueToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            return objectFieldToExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldContext.get());
        } else {
            return defaultValueToExpression(fieldTypeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> defaultValueToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                return inputValueToWhereExpression(fieldTypeContext, inputValueDefinitionContext);
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> argumentToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        if (register.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return argumentContext == null ? multiInputValueToInExpression(fieldTypeContext, inputValueDefinitionContext) : multiArgumentToExpression(fieldTypeContext, inputValueDefinitionContext, argumentContext);
        } else {
            return argumentContext == null ? singleInputValueToExpression(fieldTypeContext, inputValueDefinitionContext) : singleArgumentToExpression(fieldTypeContext, inputValueDefinitionContext, argumentContext);
        }
    }

    protected Optional<Expression> inputValueToWhereExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (register.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return multiInputValueToInExpression(fieldTypeContext, inputValueDefinitionContext);
        } else {
            return singleInputValueToExpression(fieldTypeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectFieldWithVariableToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (register.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return objectFieldWithVariableContext == null ? multiInputValueToInExpression(fieldTypeContext, inputValueDefinitionContext) : multiObjectFieldWithVariableToExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        } else {
            return objectFieldWithVariableContext == null ? singleInputValueToExpression(fieldTypeContext, inputValueDefinitionContext) : singleObjectFieldWithVariableToExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        }
    }

    protected Optional<Expression> objectFieldToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (register.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return objectFieldContext == null ? multiInputValueToInExpression(fieldTypeContext, inputValueDefinitionContext) : multiObjectFieldToExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldContext);
        } else {
            return objectFieldContext == null ? singleInputValueToExpression(fieldTypeContext, inputValueDefinitionContext) : singleObjectFieldToExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldContext);
        }
    }

    protected Optional<Expression> singleArgumentToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {

        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();

        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();

        if (fieldDefinitionContext.isPresent()) {
            String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.get().type());
            if (register.isObject(fieldTypeName)) {
                if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueWithVariableToExpression(objectTypeDefinition, fieldDefinitionContext.get(), inputValueDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable()));
                } else {
                    //todo
                }
            } else if (register.isScaLar(fieldTypeName) || register.isInnerScalar(fieldTypeName)) {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return operatorArgumentToExpression(argumentToColumn(fieldTypeContext, argumentContext), inputValueDefinitionContext, argumentContext);
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueWithVariableToMultipleExpression(fieldTypeContext, inputValueDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable()));
                } else if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return Optional.of(scalarValueWithVariableToExpression(argumentToColumn(fieldTypeContext, argumentContext), argumentContext.valueWithVariable()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleObjectFieldWithVariableToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();

        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();

        if (fieldDefinitionContext.isPresent()) {
            String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.get().type());
            if (register.isObject(fieldTypeName)) {
                if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueWithVariableToExpression(objectTypeDefinition, fieldDefinitionContext.get(), inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable()));
                } else {
                    //todo
                }
            } else if (register.isScaLar(fieldTypeName) || register.isInnerScalar(fieldTypeName)) {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return operatorObjectFieldWithVariableToExpression(objectFieldWithVariableToColumn(fieldTypeContext, objectFieldWithVariableContext), inputValueDefinitionContext, objectFieldWithVariableContext);
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueWithVariableToMultipleExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable()));
                } else if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return Optional.of(scalarValueWithVariableToExpression(objectFieldWithVariableToColumn(fieldTypeContext, objectFieldWithVariableContext), objectFieldWithVariableContext.valueWithVariable()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleObjectFieldToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();

        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();

        if (fieldDefinitionContext.isPresent()) {
            String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.get().type());
            if (register.isObject(fieldTypeName)) {
                if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueToExpression(objectTypeDefinition, fieldDefinitionContext.get(), inputValueDefinitionContext, objectFieldContext.value().objectValue()));
                } else {
                    //todo
                }
            } else if (register.isScaLar(fieldTypeName) || register.isInnerScalar(fieldTypeName)) {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return operatorObjectFieldToExpression(objectFieldToColumn(fieldTypeContext, objectFieldContext), inputValueDefinitionContext, objectFieldContext);
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueToMultipleExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldContext.value().objectValue()));
                } else if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return Optional.of(scalarValueToExpression(objectFieldToColumn(fieldTypeContext, objectFieldContext), objectFieldContext.value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleInputValueToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();

        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();

        if (fieldDefinitionContext.isPresent()) {
            String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.get().type());
            if (register.isObject(fieldTypeName)) {
                if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueToExpression(objectTypeDefinition, fieldDefinitionContext.get(), inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value().objectValue()));
                } else {
                    //todo
                }
            } else if (register.isScaLar(fieldTypeName) || register.isInnerScalar(fieldTypeName)) {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return operatorInputValueToExpression(inputValueToColumn(fieldTypeContext, inputValueDefinitionContext), inputValueDefinitionContext);
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueToMultipleExpression(fieldTypeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value().objectValue()));
                } else if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return Optional.of(scalarValueToExpression(inputValueToColumn(fieldTypeContext, inputValueDefinitionContext), inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> multiArgumentToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext == null) {
            return multiInputValueToInExpression(fieldTypeContext, inputValueDefinitionContext);
        } else {
            return multiArgumentToInExpression(fieldTypeContext, inputValueDefinitionContext, argumentContext);
        }
    }

    protected Optional<Expression> multiObjectFieldWithVariableToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (objectFieldWithVariableContext == null) {
            return multiInputValueToInExpression(fieldTypeContext, inputValueDefinitionContext);
        } else {
            return multiObjectFieldWithVariableToInExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        }
    }

    protected Optional<Expression> multiObjectFieldToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (objectFieldContext == null) {
            return multiInputValueToInExpression(fieldTypeContext, inputValueDefinitionContext);
        } else {
            return multiObjectFieldToInExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldContext);
        }
    }

    protected Optional<List<Expression>> multiConditionalArgumentsToExpressionList(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinition = argumentsDefinitionContext.inputValueDefinition().stream().filter(inputValueDefinitionContext -> register.fieldTypeIsList(inputValueDefinitionContext.type()) && isConditionalObject(inputValueDefinitionContext)).findFirst();
        if (conditionalInputValueDefinition.isPresent()) {
            Optional<GraphqlParser.ArgumentContext> argumentContext = getArgumentFromInputValueDefinition(argumentsContext, conditionalInputValueDefinition.get());
            return argumentContext.map(context -> context.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream().map(valueWithVariableContext ->
                    objectValueWithVariableToMultipleExpression(fieldTypeContext, conditionalInputValueDefinition.get(), valueWithVariableContext.objectValueWithVariable())).collect(Collectors.toList())).or(() -> multiConditionalInputValueToExpression(fieldTypeContext, conditionalInputValueDefinition.get()));
        }
        return Optional.empty();
    }

    protected Optional<List<Expression>> multiConditionalObjectValueWithVariableToExpressionList(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinition = inputObjectValueDefinitionsContext.inputValueDefinition().stream().filter(inputValueDefinitionContext1 -> register.fieldTypeIsList(inputValueDefinitionContext1.type()) && isConditionalObject(inputValueDefinitionContext1)).findFirst();
        if (conditionalInputValueDefinition.isPresent()) {
            Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, conditionalInputValueDefinition.get());
            return objectFieldWithVariableContext.map(fieldWithVariableContext -> fieldWithVariableContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream().map(valueWithVariableContext ->
                    objectValueWithVariableToMultipleExpression(fieldTypeContext, conditionalInputValueDefinition.get(), valueWithVariableContext.objectValueWithVariable())).collect(Collectors.toList())).or(() -> multiConditionalInputValueToExpression(fieldTypeContext, conditionalInputValueDefinition.get()));
        }
        return Optional.empty();
    }

    protected Optional<List<Expression>> multiConditionalObjectValueToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinition = inputObjectValueDefinitionsContext.inputValueDefinition().stream().filter(inputValueDefinitionContext1 -> register.fieldTypeIsList(inputValueDefinitionContext1.type()) && isConditionalObject(inputValueDefinitionContext1)).findFirst();
        if (conditionalInputValueDefinition.isPresent()) {
            Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = getObjectFieldFromInputValueDefinition(objectValueContext, conditionalInputValueDefinition.get());
            return objectFieldContext.map(fieldContext -> fieldContext.value().arrayValue().value().stream().map(valueContext ->
                    objectValueToMultipleExpression(fieldTypeContext, conditionalInputValueDefinition.get(), valueContext.objectValue())).collect(Collectors.toList())).or(() -> multiConditionalInputValueToExpression(fieldTypeContext, conditionalInputValueDefinition.get()));
        }
        return Optional.empty();
    }

    protected Optional<List<Expression>> multiConditionalInputValueToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                return Optional.of(inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream().map(valueWithVariableContext ->
                        objectValueToMultipleExpression(fieldTypeContext, inputValueDefinitionContext, valueWithVariableContext.objectValue())).collect(Collectors.toList()));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    private Optional<Expression> multiArgumentToInExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return multiValueWithVariableToInExpression(fieldTypeContext, inputValueDefinitionContext, argumentContext.valueWithVariable());
    }

    private Optional<Expression> multiInputValueToInExpression(GraphqlParser.TypeContext fieldTypeContext,GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return multiValueToInExpression(fieldTypeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value());
    }

    private Optional<Expression> multiObjectFieldWithVariableToInExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return multiValueWithVariableToInExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable());
    }

    private Optional<Expression> multiObjectFieldToInExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return multiValueToInExpression(fieldTypeContext, inputValueDefinitionContext, objectFieldContext.value());
    }

    protected Optional<Expression> multiValueWithVariableToInExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
            if (valueWithVariableContext.arrayValueWithVariable() != null) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(inputValueToColumn(fieldTypeContext, inputValueDefinitionContext));
                inExpression.setRightItemsList(new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream().map(this::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> multiValueToInExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
            if (valueContext.arrayValue() != null) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(inputValueToColumn(fieldTypeContext, inputValueDefinitionContext));
                inExpression.setRightItemsList(new ExpressionList(valueContext.arrayValue().value().stream().map(this::scalarValueToDBValue).collect(Collectors.toList())));
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    private boolean hasOrConditional(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        return argumentsContext.argument().stream().anyMatch(argumentContext ->
                getInputValueDefinitionFromArgumentsDefinitionContext(argumentsDefinitionContext, argumentContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, argumentContext.valueWithVariable()))
                        .orElse(false));
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {

        return objectValueWithVariableContext.objectFieldWithVariable().stream().anyMatch(objectFieldWithVariableContext ->
                getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldWithVariableContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable()))
                        .orElse(false));
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return objectValueContext.objectField().stream().anyMatch(objectFieldContext ->
                getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, objectFieldContext.value()))
                        .orElse(false));
    }

    private boolean isOrConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (isConditional(inputValueDefinitionContext)) {
            return conditionalIsOr(valueWithVariableContext.enumValue());
        }
        return false;
    }

    private boolean isOrConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        if (isConditional(inputValueDefinitionContext)) {
            return conditionalIsOr(valueContext.enumValue());
        }
        return false;
    }

    private boolean conditionalIsOr(GraphqlParser.EnumValueContext enumValueContext) {
        return enumValueContext != null && enumValueContext.enumValueName().getText().equals("OR");
    }

    private boolean isConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return inputValueDefinitionContext.type().typeName() != null && isConditional(inputValueDefinitionContext.type().typeName().name().getText());
    }

    private boolean isNotConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return !isConditional(inputValueDefinitionContext);
    }

    private boolean isConditional(String typeName) {
        return typeName != null && register.isEnum(typeName) && typeName.equals("Conditional");
    }

    private boolean isOperatorObject(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return containsEnum(inputValueDefinitionContext, "Operator");
    }

    private boolean isConditionalObject(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return containsEnum(inputValueDefinitionContext, "Conditional");
    }

    private boolean containsEnum(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, String enumName) {
        String fieldTypeName = register.getFieldTypeName(inputValueDefinitionContext.type());
        if (register.isInputObject(fieldTypeName)) {
            GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(fieldTypeName);
            return typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .anyMatch(inputValueDefinitionContext1 ->
                            register.isEnum(inputValueDefinitionContext1.type().getText()) &&
                                    inputValueDefinitionContext1.type().typeName().name().getText().equals(enumName));
        }
        return false;
    }

    private Optional<Expression> operatorArgumentToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return operatorValueWithVariableToExpression(leftExpression, inputValueDefinitionContext, argumentContext.valueWithVariable());
    }

    private Optional<Expression> operatorInputValueToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return operatorValueToExpression(leftExpression, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value());
    }

    private Optional<Expression> operatorObjectFieldWithVariableToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return operatorValueWithVariableToExpression(leftExpression, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable());
    }

    private Optional<Expression> operatorObjectFieldToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return operatorValueToExpression(leftExpression, inputValueDefinitionContext, objectFieldContext.value());
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));
        Optional<GraphqlParser.ObjectFieldWithVariableContext> enumField = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext1 ->
                        register.isEnum(inputValueDefinitionContext1.type().getText()) &&
                                inputValueDefinitionContext1.type().typeName().name().getText().equals("Operator")).findFirst().flatMap(inputValueDefinitionContext1 -> getObjectFieldWithVariableFromInputValueDefinition(valueWithVariableContext.objectValueWithVariable(), inputValueDefinitionContext1));

        Optional<GraphqlParser.InputValueDefinitionContext> valueFieldType = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext1 ->
                        !register.isEnum(inputValueDefinitionContext1.type().getText()) ||
                                !inputValueDefinitionContext1.type().typeName().name().getText().equals("Operator")).findFirst();

        if (enumField.isPresent() && valueFieldType.isPresent()) {
            Optional<GraphqlParser.ObjectFieldWithVariableContext> valueField = getObjectFieldWithVariableFromInputValueDefinition(valueWithVariableContext.objectValueWithVariable(), valueFieldType.get());
            if (valueField.isPresent()) {
                return operatorValueWithVariableToExpression(leftExpression, enumField.get().valueWithVariable().enumValue(), valueField.get().valueWithVariable());
            } else if (valueFieldType.get().type().nonNullType() != null) {
                if (valueFieldType.get().defaultValue() != null) {
                    return operatorValueToExpression(leftExpression, enumField.get().valueWithVariable().enumValue(), valueFieldType.get().defaultValue().value());
                } else {
                    //todo
                }
            }
        }
        //todo
        return Optional.empty();
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));
        Optional<GraphqlParser.ObjectFieldContext> enumField = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext1 ->
                        register.isEnum(inputValueDefinitionContext1.type().getText()) &&
                                inputValueDefinitionContext1.type().typeName().name().getText().equals("Operator")).findFirst().flatMap(inputValueDefinitionContext1 -> getObjectFieldFromInputValueDefinition(valueContext.objectValue(), inputValueDefinitionContext1));

        Optional<GraphqlParser.InputValueDefinitionContext> valueFieldType = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext1 ->
                        !register.isEnum(inputValueDefinitionContext1.type().getText()) ||
                                !inputValueDefinitionContext1.type().typeName().name().getText().equals("Operator")).findFirst();

        if (enumField.isPresent() && valueFieldType.isPresent()) {
            Optional<GraphqlParser.ObjectFieldContext> valueField = getObjectFieldFromInputValueDefinition(valueContext.objectValue(), valueFieldType.get());
            if (valueField.isPresent()) {
                return operatorValueToExpression(leftExpression, enumField.get().value().enumValue(), valueField.get().value());
            } else if (valueFieldType.get().type().nonNullType() != null) {
                if (valueFieldType.get().defaultValue() != null) {
                    return operatorValueToExpression(leftExpression, enumField.get().value().enumValue(), valueFieldType.get().defaultValue().value());
                } else {
                    //todo
                }
            }
        }
        //todo
        return Optional.empty();
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return Optional.ofNullable(operatorValueWithVariableToInExpression(leftExpression, enumValueContext, valueWithVariableContext));
        }
        return Optional.ofNullable(operatorScalarValueToExpression(leftExpression, enumValueContext, valueWithVariableContext.StringValue(), valueWithVariableContext.IntValue(), valueWithVariableContext.FloatValue(), valueWithVariableContext.BooleanValue(), valueWithVariableContext.NullValue()));
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, GraphqlParser.ValueContext valueContext) {
        if (valueContext.arrayValue() != null) {
            return Optional.ofNullable(operatorValueToInExpression(leftExpression, enumValueContext, valueContext));
        }
        return Optional.ofNullable(operatorScalarValueToExpression(leftExpression, enumValueContext, valueContext.StringValue(), valueContext.IntValue(), valueContext.FloatValue(), valueContext.BooleanValue(), valueContext.NullValue()));
    }

    private Expression operatorValueToInExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, GraphqlParser.ValueContext valueContext) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(leftExpression);
        inExpression.setRightItemsList(new ExpressionList(valueContext.arrayValue().value().stream().map(this::scalarValueToDBValue).collect(Collectors.toList())));
        if ("IN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(false);
        } else if ("NIN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(true);
        } else {
            //todo
            return null;
        }
        return inExpression;
    }

    private Expression operatorValueWithVariableToInExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(leftExpression);
        inExpression.setRightItemsList(new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream().map(this::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
        if ("IN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(false);
        } else if ("NIN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(true);
        } else {
            //todo
            return null;
        }
        return inExpression;
    }

    private Expression operatorScalarValueToExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        switch (enumValueContext.enumValueName().getText()) {
            case "EQ":
                return scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue);
            case "NEQ":
                return new NotExpression(scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue));
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return likeExpression;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return notLikeExpression;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return greaterThan;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return greaterThanEquals;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return minorThan;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return minorThanEquals;
            case "NIL":
                IsNullExpression isNullExpression = new IsNullExpression();
                isNullExpression.setLeftExpression(leftExpression);
                return isNullExpression;
            case "NNIL":
                IsNullExpression isNotNullExpression = new IsNullExpression();
                isNotNullExpression.setNot(true);
                isNotNullExpression.setLeftExpression(leftExpression);
                return isNotNullExpression;
            default:
                return null;
        }
    }

    protected Expression objectValueWithVariableToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.type());
        ExistsExpression expression = new ExistsExpression();
        SubSelect subSelect = new SubSelect();
        PlainSelect body = new PlainSelect();
        body.setSelectItems(Collections.singletonList(new AllColumns()));
        Table table = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(objectTypeDefinitionContext.name().getText()));
        Table subTable = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(fieldTypeName));
        body.setFromItem(subTable);
        EqualsTo idEqualsTo = new EqualsTo();
        if (register.fieldTypeIsList(fieldDefinitionContext.type())) {
            idEqualsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(fieldTypeName, objectTypeDefinitionContext.name().getText()))));
            idEqualsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(objectTypeDefinitionContext.name().getText()))));
        } else {
            idEqualsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(fieldTypeName))));
            idEqualsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
        }
        Expression subWhereExpression = objectValueWithVariableToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueWithVariableContext);
        MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(idEqualsTo, subWhereExpression));
        body.setWhere(multiAndExpression);
        subSelect.setSelectBody(body);
        expression.setRightExpression(subSelect);
        return expression;
    }

    protected Expression objectValueToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.type());
        ExistsExpression expression = new ExistsExpression();
        SubSelect subSelect = new SubSelect();
        PlainSelect body = new PlainSelect();
        body.setSelectItems(Collections.singletonList(new AllColumns()));
        Table table = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(objectTypeDefinitionContext.name().getText()));
        Table subTable = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(fieldTypeName));
        body.setFromItem(subTable);
        EqualsTo idEqualsTo = new EqualsTo();
        if (register.fieldTypeIsList(fieldDefinitionContext.type())) {
            idEqualsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(fieldTypeName, objectTypeDefinitionContext.name().getText()))));
            idEqualsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(objectTypeDefinitionContext.name().getText()))));
        } else {
            idEqualsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(fieldTypeName))));
            idEqualsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
        }
        Expression subWhereExpression = objectValueToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueContext);
        MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(idEqualsTo, subWhereExpression));
        body.setWhere(multiAndExpression);
        subSelect.setSelectBody(body);
        expression.setRightExpression(subSelect);
        return expression;
    }

    private Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromArgumentsDefinitionContext(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return argumentsDefinitionContext.inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(argumentContext.name().getText())).findFirst();
    }

    private Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldWithVariableContext.name().getText())).findFirst();
    }

    private Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldContext.name().getText())).findFirst();
    }

    private Optional<GraphqlParser.ArgumentContext> getArgumentFromInputValueDefinition(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    private Optional<GraphqlParser.ObjectFieldWithVariableContext> getObjectFieldWithVariableFromInputValueDefinition(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream().filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    private Optional<GraphqlParser.ObjectFieldContext> getObjectFieldFromInputValueDefinition(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return objectValueContext.objectField().stream().filter(objectFieldContext -> objectFieldContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    protected Column argumentToColumn(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentContext argumentContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.name().getText()));
    }

    protected Column inputValueToColumn(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText()));
    }

    protected Column objectFieldWithVariableToColumn(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(objectFieldWithVariableContext.name().getText()));
    }

    protected Column objectFieldToColumn(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(objectFieldContext.name().getText()));
    }

    protected Expression scalarValueToExpression(Expression leftExpression, GraphqlParser.ValueContext valueContext) {
        return scalarValueToExpression(leftExpression,
                valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue());
    }

    protected Expression scalarValueWithVariableToExpression(Expression leftExpression, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        return scalarValueToExpression(leftExpression,
                valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue());
    }

    protected Expression scalarValueToExpression(Expression leftExpression, TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        if (stringValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new StringValue(CharMatcher.is('"').trimFrom(stringValue.getText())));
            return equalsTo;
        } else if (intValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new LongValue(intValue.getText()));
            return equalsTo;
        } else if (floatValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new DoubleValue(floatValue.getText()));
            return equalsTo;
        } else if (booleanValue != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(leftExpression);
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(booleanValue.getText()));
            return isBooleanExpression;
        } else if (nullValue != null) {
            IsNullExpression isNullExpression = new IsNullExpression();
            isNullExpression.setLeftExpression(leftExpression);
            return isNullExpression;
        }
        return null;
    }

    protected Expression scalarValueToDBValue(GraphqlParser.ValueContext valueContext) {
        return scalarValueToDBValue(valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue());
    }

    protected Expression scalarValueWithVariableToDBValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        return scalarValueToDBValue(valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue());
    }

    protected Expression scalarValueToDBValue(TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        if (stringValue != null) {
            return new StringValue(CharMatcher.is('"').trimFrom(stringValue.getText()));
        } else if (intValue != null) {
            return new LongValue(intValue.getText());
        } else if (floatValue != null) {
            return new DoubleValue(floatValue.getText());
        } else if (booleanValue != null) {
            //todo
        } else if (nullValue != null) {
            return new NullValue();
        }
        return null;
    }
}
