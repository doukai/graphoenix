package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

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
            body.setSelectItems(operationDefinitionContext.selectionSet().selection().stream().map(this::createQueryFieldSubSelect).collect(Collectors.toList()));

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

    protected SelectExpressionItem createQueryFieldSubSelect(GraphqlParser.SelectionContext selectionContext) {

        Optional<GraphqlParser.TypeContext> typeContext = graphqlAntlrRegister.getQueryObjectFieldType(selectionContext.field().name().getText());

        if (typeContext.isPresent()) {
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();

            Function function = new Function();
            function.setName(createJsonFunctionName(typeContext.get()));
            function.setParameters(new ExpressionList(createJsonFunctionParameters(selectionContext.field().selectionSet().selection())));

            selectExpressionItem.setExpression(function);

            SubSelect subSelect = new SubSelect();
            PlainSelect body = new PlainSelect();
            body.setSelectItems(Collections.singletonList(selectExpressionItem));
            subSelect.setSelectBody(body);
            return selectExpressionItem;
        }

        return null;
    }


    protected String createJsonFunctionName(GraphqlParser.TypeContext typeContext) {
        if (graphqlAntlrRegister.fieldTypeIsList(typeContext)) {
            return "JSON_ARRAYAGG";
        } else {
            return "JSON_OBJECT";
        }
    }


    protected List<Expression> createJsonFunctionParameters(List<GraphqlParser.SelectionContext> selectionContexts) {

        return selectionContexts.stream().map(selectionContext -> new Column(new Table(""), selectionContext.field().name().getText())).collect(Collectors.toList());
    }

}
