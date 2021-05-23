package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphqlAntlrToSelect {

    final private GraphqlAntlrRegister graphqlAntlrRegister;

    public GraphqlAntlrToSelect(GraphqlAntlrRegister graphqlAntlrRegister) {
        this.graphqlAntlrRegister = graphqlAntlrRegister;
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
            Table table = new Table("dual");
            body.setFromItem(table);

            Function function = new Function();
            function.setName("JSON_OBJECT");
            function.setParameters(new ExpressionList(operationDefinitionContext.selectionSet().selection().stream().map(this::createQueryFieldSubSelect).collect(Collectors.toList())));

            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
//            selectExpressionItem.setAlias(new Alias(operationDefinitionContext.name().getText()));

            selectExpressionItem.setExpression(function);
            body.setSelectItems(Collections.singletonList(selectExpressionItem));

            if (operationDefinitionContext.name() != null) {
                operationDefinitionContext.name().getText();
            }

            if (operationDefinitionContext.variableDefinitions() != null) {
                //TODO
            }
            if (operationDefinitionContext.directives() != null) {
                //TODO
            }
            select.setSelectBody(body);
            return Optional.of(select);
        }
        return Optional.empty();
    }

    protected Expression createQueryFieldSubSelect(GraphqlParser.SelectionContext selectionContext) {

        Optional<GraphqlParser.TypeContext> typeContext = graphqlAntlrRegister.getQueryObjectFieldType(selectionContext.field().name().getText());

        if (typeContext.isPresent()) {

            SubSelect subSelect = new SubSelect();
            PlainSelect body = new PlainSelect();
            SelectExpressionItem subSelectExpressionItem = new SelectExpressionItem();

            Table table = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(graphqlAntlrRegister.getFieldTypeName(typeContext.get())));

            subSelectExpressionItem.setExpression(createJsonFunction(typeContext.get(), selectionContext));
            subSelectExpressionItem.setAlias(new Alias(selectionContext.field().name().getText()));

            body.setSelectItems(Collections.singletonList(subSelectExpressionItem));
            subSelect.setSelectBody(body);
            body.setFromItem(table);

            return subSelect;
        }

        return null;
    }


    protected Function createJsonFunction(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        Table table = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(graphqlAntlrRegister.getFieldTypeName(typeContext)));
        Function function = new Function();
        if (graphqlAntlrRegister.fieldTypeIsList(typeContext)) {
            function.setName("JSON_ARRAYAGG");
            function.setParameters(new ExpressionList(createJsonArrayFunctionParameters(table, selectionContext.field().selectionSet().selection())));
        } else {
            function.setName("JSON_OBJECT");
            function.setParameters(new ExpressionList(createJsonObjectFunctionParameters(table, selectionContext.field().selectionSet().selection())));
        }
        return function;
    }


    protected List<Expression> createJsonArrayFunctionParameters(Table table, List<GraphqlParser.SelectionContext> selectionContexts) {


        return selectionContexts.stream()
                .map(selectionContext -> new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(selectionContext.field().name().getText()))).collect(Collectors.toList());
    }

    protected List<Expression> createJsonObjectFunctionParameters(Table table, List<GraphqlParser.SelectionContext> selectionContexts) {


        return selectionContexts.stream()
                .map(selectionContext -> new ExpressionList(new StringValue(selectionContext.field().name().getText()), new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(selectionContext.field().name().getText()))))
                .map(ExpressionList::getExpressions).flatMap(Collection::stream).collect(Collectors.toList());
    }

}
