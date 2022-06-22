package io.graphoenix.core.handler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class BaseOperationHandler {

    private final GsonBuilder gsonBuilder;

    private final Map<String, BiFunction<JsonElement, GraphqlParser.SelectionContext, Mono<JsonElement>>> operationHandlers;

    public BaseOperationHandler() {
        this.gsonBuilder = new GsonBuilder();
        this.operationHandlers = new HashMap<>();
    }

    private BiFunction<JsonElement, GraphqlParser.SelectionContext, Mono<JsonElement>> getOperationHandler(String name) {
        return operationHandlers.get(name);
    }

    protected void put(String name, BiFunction<JsonElement, GraphqlParser.SelectionContext, JsonElement> biFunction) {
        operationHandlers.put(name, (jsonElement, selectionContext) -> Mono.just(biFunction.apply(jsonElement, selectionContext)));
    }

    protected void putMono(String name, BiFunction<JsonElement, GraphqlParser.SelectionContext, Mono<JsonElement>> biMonoFunction) {
        operationHandlers.put(name, biMonoFunction);
    }

    protected Mono<JsonElement> invoke(JsonElement jsonElement, GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return Flux.fromIterable(operationDefinitionContext.selectionSet().selection())
                .flatMap(selectionContext ->
                        getOperationHandler(selectionContext.field().name().getText())
                                .apply(jsonElement.getAsJsonObject().get(selectionContext.field().name().getText()), selectionContext)
                                .map(jsonElement1 -> new AbstractMap.SimpleEntry<>(selectionContext.field().name().getText(), jsonElement1))
                )
                .collectList()
                .map(entryList -> {
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.entrySet().addAll(entryList);
                            return jsonObject;
                        }
                );
    }

    protected <T> T getArgument(GraphqlParser.SelectionContext selectionContext, String name, Class<T> beanClass) {
        return selectionContext.field().arguments().argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals(name))
                .findFirst()
                .map(argumentContext -> gsonBuilder.create().fromJson(argumentContext.valueWithVariable().getText(), beanClass))
                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SELECTION_ARGUMENT_NOT_EXIST.bind(name, selectionContext.field().name().getText())));
    }
}
