package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Inject;
import java.util.Arrays;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class GraphQLSelectionFilter {

    private final String[] excludeDirectiveNames = new String[]{"invoke"};

    private final IGraphQLDocumentManager manager;

    @Inject
    public GraphQLSelectionFilter(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public GraphqlParser.OperationDefinitionContext filterSelections(String graphQL) {
        return filterSelections(DOCUMENT_UTIL.graphqlToOperation(graphQL));
    }

    public GraphqlParser.OperationDefinitionContext filterSelections(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        operationDefinitionContext.selectionSet().selection().forEach(selectionContext -> filterSelection(selectionContext, operationDefinitionContext));
        return operationDefinitionContext;
    }

    private void filterSelection(GraphqlParser.SelectionContext selectionContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (selectionContext.field() != null) {
            if (operationDefinitionContext.operationType().QUERY() != null) {
                filterSelection(selectionContext, manager.getObject(manager.getQueryOperationTypeName().orElseThrow()).orElseThrow());
            } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                filterSelection(selectionContext, manager.getObject(manager.getMutationOperationTypeName().orElseThrow()).orElseThrow());
            }
        }
    }

    private void filterSelection(GraphqlParser.SelectionContext selectionContext, GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(objectTypeDefinitionContext.name().getText(), selectionContext.field().name().getText()).orElseThrow();
        if (fieldDefinitionContext.directives().directive().stream()
                .anyMatch(directiveContext ->
                        Arrays.stream(excludeDirectiveNames)
                                .anyMatch(excludeDirectiveName -> excludeDirectiveName.equals(directiveContext.name().getText()))
                )
        ) {
            selectionContext.field().removeLastChild();
        }

        if (selectionContext.field().selectionSet() != null) {
            selectionContext.field().selectionSet().selection().forEach(subSelectionContext -> filterSelection(subSelectionContext, manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type())).orElseThrow()));
        }
    }
}
