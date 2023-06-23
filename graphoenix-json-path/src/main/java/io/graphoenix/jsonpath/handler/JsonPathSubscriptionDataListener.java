package io.graphoenix.jsonpath.handler;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JakartaJsonProvider;
import com.jayway.jsonpath.spi.mapper.JakartaMappingProvider;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.jsonpath.expression.Expression;
import io.graphoenix.jsonpath.translator.GraphQLArgumentsToFilter;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.core.handler.SubscriptionDataListener;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.*;

import java.util.*;

@Dependent
public class JsonPathSubscriptionDataListener extends SubscriptionDataListener {

    private final IGraphQLDocumentManager manager;

    private final GraphQLArgumentsToFilter argumentsToFilter;

    private final Configuration configuration;

    private final Map<String, List<String>> filterMap = new HashMap<>();

    @Inject
    public JsonPathSubscriptionDataListener(IGraphQLDocumentManager manager, GraphQLArgumentsToFilter argumentsToFilter) {
        this.manager = manager;
        this.argumentsToFilter = argumentsToFilter;
        this.configuration = Configuration.builder()
                .jsonProvider(new JakartaJsonProvider())
                .mappingProvider(new JakartaMappingProvider())
                .options(EnumSet.noneOf(Option.class))
                .build();
    }

    @Override
    public boolean merged(JsonValue jsonValue) {
        JsonObject jsonObject = jsonValue.asJsonObject();
        String typeName = jsonObject.getString("type");
        JsonValue arguments = jsonObject.get("arguments");
        JsonValue mutation = jsonObject.get("mutation");
        return merged(typeName, arguments.asJsonArray()) || merged(typeName, JsonPath.using(configuration).parse(mutation.asJsonArray()));
    }

    private boolean merged(String typeName, DocumentContext documentContext) {
        if (filterMap.get(typeName) != null) {
            for (String filter : filterMap.get(typeName)) {
                List<JsonValue> jsonValueList = documentContext.read(filter);
                if (jsonValueList.size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public JsonPathSubscriptionDataListener indexFilter(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String subscriptionTypeName = manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));
        for (GraphqlParser.SelectionContext selectionContext : operationDefinitionContext.selectionSet().selection()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(subscriptionTypeName, selectionContext.field().name().getText())
                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(subscriptionTypeName, selectionContext.field().name().getText())));
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                if (selectionContext.field().arguments() != null) {
                    Optional<Expression> expression = argumentsToFilter.argumentsToMultipleExpression(fieldDefinitionContext.argumentsDefinition(), selectionContext.field().arguments());
                    if (expression.isPresent()) {
                        filterMap.computeIfAbsent(fieldTypeName, k -> new ArrayList<>());
                        filterMap.get(fieldTypeName).add("$[?" + expression.get() + "]");
                    }
                }
            }
        }
        return this;
    }
}
