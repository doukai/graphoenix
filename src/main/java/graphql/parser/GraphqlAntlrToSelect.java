package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

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

        Select select = new Select();
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().getText().equals("query")) {

            if (operationDefinitionContext.name() != null) {
                operationDefinitionContext.name().getText();
            }

            if (operationDefinitionContext.selectionSet() != null) {
                select.setSelectBody(createPlainSelect(operationDefinitionContext.selectionSet()));
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

    protected PlainSelect createPlainSelect(GraphqlParser.SelectionSetContext selectionSetContext) {
        PlainSelect body = new PlainSelect();

        if (selectionSetContext != null) {
            body.setSelectItems(selectionSetContext.selection().stream().map(this::createSelectItem).collect(Collectors.toList()));
        }
        return body;
    }

    protected SelectItem createSelectItem(GraphqlParser.SelectionContext selectionContext) {
        if (selectionContext.field() == null) {

            return null;
        }

        return createSelectItem(selectionContext.field());
    }


    protected SelectItem createSelectItem(GraphqlParser.FieldContext fieldContext) {

        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = graphqlAntlrRegister.getDefinition(graphqlAntlrRegister.getQuerySchemaTypeName());
        Table table = new Table(typeDefinitionContext.objectTypeDefinition().name().getText());
        Column column = new Column(table, fieldContext.name().getText());
        selectExpressionItem.setExpression(column);
        if (fieldContext.alias() != null) {
            Alias alias = new Alias(fieldContext.alias().getText(), true);
            selectExpressionItem.setAlias(alias);
        }
        return selectExpressionItem;
    }
}
