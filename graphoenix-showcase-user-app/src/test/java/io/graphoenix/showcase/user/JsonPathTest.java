package io.graphoenix.showcase.user;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.jsonpath.translator.expression.Expression;
import io.graphoenix.jsonpath.translator.translator.GraphQLArgumentsToFilter;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class JsonPathTest {

    private final DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
    private final IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
    private final GraphQLArgumentsToFilter filter = BeanContext.get(GraphQLArgumentsToFilter.class);
    private final String graphQL = "{\n" +
            "  userList(name:{val:\"name\"} age:{opr:GT val:11} roles:{name:{opr:NLK val:\"%role%\"} type:{opr:IN in:[ADMIN,USER]}}){\n" +
            "    name\n" +
            "  }\n" +
            "}";

    @Test
    void testTranslator() throws IOException, URISyntaxException {
        documentBuilder.startupManager();
        GraphqlParser.OperationDefinitionContext operationDefinitionContext = DOCUMENT_UTIL.graphqlToOperation(graphQL);
        GraphqlParser.SelectionContext selectionContext = operationDefinitionContext.selectionSet().selection(0);
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getObjectFieldDefinition("QueryType", selectionContext.field().name().getText())
                .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind("QueryType", selectionContext.field().name().getText())));

        Optional<Expression> expression = filter.argumentsToMultipleExpression(fieldDefinitionContext.argumentsDefinition(), selectionContext.field().arguments());
        expression.ifPresent(System.out::println);
    }
}
