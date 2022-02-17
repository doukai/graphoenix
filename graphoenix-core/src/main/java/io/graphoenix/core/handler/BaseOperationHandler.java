package io.graphoenix.core.handler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.core.error.GraphQLErrorType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class BaseOperationHandler {

    private final GsonBuilder gsonBuilder;

    private final Map<String, BiFunction<JsonElement, GraphqlParser.SelectionContext, JsonElement>> operationHandlers;

    public BaseOperationHandler() {
        this.gsonBuilder = new GsonBuilder();
        this.operationHandlers = new HashMap<>();
    }

    private BiFunction<JsonElement, GraphqlParser.SelectionContext, JsonElement> getOperationHandler(String name) {
        return operationHandlers.get(name);
    }

    protected void put(String name, BiFunction<JsonElement, GraphqlParser.SelectionContext, JsonElement> biFunction) {
        operationHandlers.put(name, biFunction);
    }

    protected JsonElement invoke(JsonElement jsonElement, GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        JsonObject jsonObject = new JsonObject();
        operationDefinitionContext.selectionSet().selection()
                .forEach(selectionContext ->
                        jsonObject.add(
                                selectionContext.field().name().getText(),
                                getOperationHandler(selectionContext.field().name().getText())
                                        .apply(jsonElement.getAsJsonObject().get(selectionContext.field().name().getText()), selectionContext)
                        )
                );
        return jsonObject;
    }

    protected <T> T getArgument(GraphqlParser.SelectionContext selectionContext, String name, Class<T> beanClass) {
        return selectionContext.field().arguments().argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals(name))
                .findFirst()
                .map(argumentContext -> gsonBuilder.create().fromJson(argumentContext.valueWithVariable().getText(), beanClass))
                .orElseThrow(() -> new GraphQLProblem(GraphQLErrorType.SELECTION_ARGUMENT_NOT_EXIST.bind(name, selectionContext.field().name().getText())));
    }
}
