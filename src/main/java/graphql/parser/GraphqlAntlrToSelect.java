package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;

import java.util.*;
import java.util.stream.Collectors;

public class GraphqlAntlrToSelect {

    final private GraphqlAntlrRegister register;

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
        return createSelect(definitionContext.operationDefinition());
    }

    protected Optional<Select> createSelect(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {

        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().getText().equals("query")) {
            Select select = new Select();

            PlainSelect body = new PlainSelect();

            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
            selectExpressionItem.setExpression(createJsonObjectFunction(null, operationDefinitionContext.selectionSet().selection()));
            body.setSelectItems(Collections.singletonList(selectExpressionItem));

            Table table = new Table("dual");
            body.setFromItem(table);

            select.setSelectBody(body);

            if (operationDefinitionContext.name() != null) {
                operationDefinitionContext.name().getText();
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

    protected Expression createFieldSubSelect(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {

        String typeName = typeContext == null ? register.getQueryTypeName() : register.getFieldTypeName(typeContext);
        String filedTypeName = register.getObjectFieldTypeName(typeName, selectionContext.field().name().getText());

        if (register.isObject(filedTypeName)) {

            Optional<GraphqlParser.TypeContext> fieldTypeContext = register.getObjectFieldTypeContext(typeName, selectionContext.field().name().getText());
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText());
            if (fieldTypeContext.isPresent()) {
                SubSelect subSelect = new SubSelect();
                PlainSelect body = new PlainSelect();
                SelectExpressionItem selectExpressionItem = new SelectExpressionItem();

                selectExpressionItem.setExpression(createJsonFunction(fieldTypeContext.get(), selectionContext));

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
                        body.setWhere(createWhere(fieldTypeContext.get(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments()));
                    }
                }

                return subSelect;
            }
        } else if (register.isScaLar(filedTypeName) || register.isInnerScalar(filedTypeName)) {
            if (typeContext != null) {
                String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
                Table table = new Table(tableName);
                return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(selectionContext.field().name().getText()));
            }
        }

        return null;
    }


    protected Function createJsonFunction(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        if (register.fieldTypeIsList(typeContext)) {
            return createJsonArrayFunction(typeContext, selectionContext.field().selectionSet().selection());
        } else {
            return createJsonObjectFunction(typeContext, selectionContext.field().selectionSet().selection());
        }
    }

    protected Function createJsonArrayFunction(GraphqlParser.TypeContext typeContext, List<GraphqlParser.SelectionContext> selectionContexts) {

        Function function = new Function();
        function.setName("JSON_ARRAYAGG");
        function.setParameters(new ExpressionList(createJsonObjectFunction(typeContext, selectionContexts)));

        return function;
    }

    protected Function createJsonObjectFunction(GraphqlParser.TypeContext typeContext, List<GraphqlParser.SelectionContext> selectionContexts) {

        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(new ExpressionList(selectionContexts.stream()
                .map(selectionContext -> new ExpressionList(new StringValue(selectionContext.field().name().getText()), createFieldSubSelect(typeContext, selectionContext)))
                .map(ExpressionList::getExpressions).flatMap(Collection::stream).collect(Collectors.toList())));

        return function;
    }

    protected Expression createWhere(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        if (hasOrConditional(argumentsDefinitionContext, argumentsContext)) {

            return new MultiOrExpression(createExpressions(fieldTypeContext, argumentsDefinitionContext, argumentsContext));
        }

        return new MultiAndExpression(createExpressions(fieldTypeContext, argumentsDefinitionContext, argumentsContext));
    }

    protected List<Expression> createExpressions(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        return argumentsDefinitionContext.inputValueDefinition().stream().map(inputValueDefinitionContext -> createExpression(fieldTypeContext, inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    private boolean hasOrConditional(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        return argumentsContext.argument().stream().anyMatch(argumentContext -> getArgumentDefinition(argumentsDefinitionContext, argumentContext).map(inputValueDefinitionContext -> isOrConditional(inputValueDefinitionContext, argumentContext)).orElse(false));
    }

    private boolean isOrConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {

        if (isConditionalInputValue(inputValueDefinitionContext)) {
            return argumentContext.valueWithVariable().enumValue() != null && argumentContext.valueWithVariable().enumValue().enumValueName().getText().equals("OR");
        }
        return false;
    }

    private boolean isConditionalInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {

        return inputValueDefinitionContext.type().typeName() != null && register.isEnum(inputValueDefinitionContext.type().typeName().name().getText()) && inputValueDefinitionContext.type().typeName().name().getText().equals("Conditional");
    }

    private Optional<GraphqlParser.InputValueDefinitionContext> getArgumentDefinition(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {

        return argumentsDefinitionContext.inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(argumentContext.name().getText())).findFirst();

    }

    private Optional<GraphqlParser.ArgumentContext> getArgumentDefinition(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();

    }

    protected Optional<Expression> createExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        if (isConditionalInputValue(inputValueDefinitionContext)) {
            return Optional.empty();
        }
        Optional<GraphqlParser.ArgumentContext> argumentContext = getArgumentDefinition(inputValueDefinitionContext, argumentsContext);
        if (argumentContext.isPresent()) {
            return typeToExpression(fieldTypeContext, inputValueDefinitionContext.type(), argumentContext.get());
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null && inputValueDefinitionContext.defaultValue() != null) {
                if (inputValueDefinitionContext.type().nonNullType().typeName() != null) {
                    return defaultValueToExpression(fieldTypeContext, inputValueDefinitionContext);
                } else if (inputValueDefinitionContext.type().nonNullType().listType() != null) {
                    return defaultValueToInExpression(fieldTypeContext, inputValueDefinitionContext);
                }
            } else if (inputValueDefinitionContext.type().nonNullType() != null && inputValueDefinitionContext.defaultValue() == null) {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> typeToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentContext argumentContext) {
        if (typeContext.typeName() != null) {
            return typeNameToExpression(fieldTypeContext, argumentContext);
        } else if (typeContext.listType() != null) {
            return typeNameToInExpression(fieldTypeContext, argumentContext);
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return typeNameToExpression(fieldTypeContext, argumentContext);
            } else if (typeContext.nonNullType().listType() != null) {
                return typeNameToInExpression(fieldTypeContext, argumentContext);
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> typeNameToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentContext argumentContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        if (argumentContext.valueWithVariable().StringValue() != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.name().getText())));
            equalsTo.setRightExpression(new StringValue(argumentContext.valueWithVariable().StringValue().getText()));
            return Optional.of(equalsTo);
        } else if (argumentContext.valueWithVariable().IntValue() != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.name().getText())));
            equalsTo.setRightExpression(new StringValue(argumentContext.valueWithVariable().IntValue().getText()));
            return Optional.of(equalsTo);
        } else if (argumentContext.valueWithVariable().FloatValue() != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.name().getText())));
            equalsTo.setRightExpression(new StringValue(argumentContext.valueWithVariable().FloatValue().getText()));
            return Optional.of(equalsTo);
        } else if (argumentContext.valueWithVariable().BooleanValue() != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.name().getText())));
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(argumentContext.valueWithVariable().BooleanValue().getText()));
            return Optional.of(isBooleanExpression);
        } else if (argumentContext.valueWithVariable().NullValue() != null) {
            IsNullExpression isNullExpression = new IsNullExpression();
            isNullExpression.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.name().getText())));
            return Optional.of(isNullExpression);
        }
        return Optional.empty();
    }

    protected Optional<Expression> typeNameToInExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext.valueWithVariable().arrayValueWithVariable() != null) {
            String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
            Table table = new Table(tableName);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.name().getText())));
            inExpression.setRightItemsList(new ExpressionList(argumentContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream().map(this::arrayValueToExpression).collect(Collectors.toList())));
        }
        return Optional.empty();
    }

    protected Expression arrayValueToExpression(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.StringValue() != null) {
            return new StringValue(valueWithVariableContext.StringValue().getText());
        } else if (valueWithVariableContext.IntValue() != null) {
            return new LongValue(valueWithVariableContext.IntValue().getText());
        } else if (valueWithVariableContext.FloatValue() != null) {
            return new DoubleValue(valueWithVariableContext.IntValue().getText());
        } else if (valueWithVariableContext.BooleanValue() != null) {
            //todo
        } else if (valueWithVariableContext.NullValue() != null) {
            //todo
        }
        return null;
    }

    protected Optional<Expression> defaultValueToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        if (inputValueDefinitionContext.defaultValue().value().StringValue() != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            equalsTo.setRightExpression(new StringValue(inputValueDefinitionContext.defaultValue().value().StringValue().getText()));
            return Optional.of(equalsTo);
        } else if (inputValueDefinitionContext.defaultValue().value().IntValue() != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            equalsTo.setRightExpression(new StringValue(inputValueDefinitionContext.defaultValue().value().IntValue().getText()));
            return Optional.of(equalsTo);
        } else if (inputValueDefinitionContext.defaultValue().value().FloatValue() != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            equalsTo.setRightExpression(new StringValue(inputValueDefinitionContext.defaultValue().value().FloatValue().getText()));
            return Optional.of(equalsTo);
        } else if (inputValueDefinitionContext.defaultValue().value().BooleanValue() != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(inputValueDefinitionContext.defaultValue().value().BooleanValue().getText()));
            return Optional.of(isBooleanExpression);
        } else if (inputValueDefinitionContext.defaultValue().value().NullValue() != null) {
            IsNullExpression isNullExpression = new IsNullExpression();
            isNullExpression.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            return Optional.of(isNullExpression);
        }
        return Optional.empty();
    }

    protected Optional<Expression> defaultValueToInExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.defaultValue().value().arrayValue() != null) {
            String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
            Table table = new Table(tableName);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            inExpression.setRightItemsList(new ExpressionList(inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream().map(this::arrayValueToExpression).collect(Collectors.toList())));
        }
        return Optional.empty();
    }

    protected Expression arrayValueToExpression(GraphqlParser.ValueContext valueContext) {
        if (valueContext.StringValue() != null) {
            return new StringValue(valueContext.StringValue().getText());
        } else if (valueContext.IntValue() != null) {
            return new LongValue(valueContext.IntValue().getText());
        } else if (valueContext.FloatValue() != null) {
            return new DoubleValue(valueContext.IntValue().getText());
        } else if (valueContext.BooleanValue() != null) {
            //todo
        } else if (valueContext.NullValue() != null) {
            //todo
        }
        return null;
    }
}
