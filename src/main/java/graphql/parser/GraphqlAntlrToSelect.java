package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
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
            function.setParameters(new ExpressionList(createJsonObjectFunction(null, operationDefinitionContext.selectionSet().selection())));

            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();

            selectExpressionItem.setExpression(function);
            body.setSelectItems(Collections.singletonList(selectExpressionItem));
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

    protected Expression createQueryFieldSubSelect(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {

        String typeName = typeContext == null ? graphqlAntlrRegister.getQueryTypeName() : graphqlAntlrRegister.getFieldTypeName(typeContext);

        String filedTypeName = graphqlAntlrRegister.getObjectFieldTypeName(typeName, selectionContext.field().name().getText());

        String tableName = typeContext == null ? "dual" : DBNameConverter.INSTANCE.graphqlTypeNameToTableName(graphqlAntlrRegister.getFieldTypeName(typeContext));
        Table table = new Table(tableName);

        if (graphqlAntlrRegister.isObject(filedTypeName)) {

            Optional<GraphqlParser.TypeContext> fieldTypeContext = graphqlAntlrRegister.getObjectFieldTypeContext(typeName, selectionContext.field().name().getText());
            if (fieldTypeContext.isPresent()) {
                SubSelect subSelect = new SubSelect();
                PlainSelect body = new PlainSelect();
                SelectExpressionItem selectExpressionItem = new SelectExpressionItem();

                selectExpressionItem.setExpression(createJsonFunction(fieldTypeContext.get(), selectionContext));

                body.setSelectItems(Collections.singletonList(selectExpressionItem));
                subSelect.setSelectBody(body);

                body.setFromItem(new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(graphqlAntlrRegister.getFieldTypeName(fieldTypeContext.get()))));

                return subSelect;
            }
        } else if (graphqlAntlrRegister.isScaLar(filedTypeName) || graphqlAntlrRegister.isInnerScalar(filedTypeName)) {

            return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(selectionContext.field().name().getText()));
        }

        return null;
    }


    protected Function createJsonFunction(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        if (graphqlAntlrRegister.fieldTypeIsList(typeContext)) {
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
                .map(selectionContext -> new ExpressionList(new StringValue(selectionContext.field().name().getText()), createQueryFieldSubSelect(typeContext, selectionContext)))
                .map(ExpressionList::getExpressions).flatMap(Collection::stream).collect(Collectors.toList())));

        return function;
    }

}
