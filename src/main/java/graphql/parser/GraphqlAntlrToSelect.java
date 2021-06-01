package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
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
                }
                if (fieldDefinitionContext.isPresent() && selectionContext.field().arguments() != null) {
                    createWhere(fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments());
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

    protected Expression createWhere(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        if (hasOrConditional(argumentsDefinitionContext, argumentsContext)) {

            return new MultiOrExpression(createExpressions(argumentsDefinitionContext, argumentsContext));
        }

        return new MultiAndExpression(createExpressions(argumentsDefinitionContext, argumentsContext));
    }

    protected List<Expression> createExpressions(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        return argumentsContext.argument().stream().map(argumentContext -> createExpression(argumentsDefinitionContext, argumentContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

    }

    private boolean hasOrConditional(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        return argumentsContext.argument().stream().anyMatch(argumentContext -> getArgumentDefinition(argumentsDefinitionContext, argumentContext).map(inputValueDefinitionContext -> isOrConditional(inputValueDefinitionContext, argumentContext)).orElse(false));
    }

    private boolean isOrConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {

        if (inputValueDefinitionContext.type().typeName() != null && register.isEnum(inputValueDefinitionContext.type().typeName().name().getText()) && inputValueDefinitionContext.type().typeName().name().getText().equals("Conditional")) {
            return argumentContext.valueWithVariable().enumValue() != null && argumentContext.valueWithVariable().enumValue().enumValueName().getText().equals("OR");
        }
        return false;
    }

    private Optional<GraphqlParser.InputValueDefinitionContext> getArgumentDefinition(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {

        return argumentsDefinitionContext.inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(argumentContext.name().getText())).findFirst();

    }

    protected Optional<Expression> createExpression(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {

        Optional<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContext = getArgumentDefinition(argumentsDefinitionContext, argumentContext);
        if(inputValueDefinitionContext.isPresent()){

        }

        return Optional.empty();
    }

}
