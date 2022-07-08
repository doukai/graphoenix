package io.graphoenix.core.handler;

import com.google.gson.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;

public abstract class BaseOperationHandler {

    private final GsonBuilder gsonBuilder;

    private final Map<String, BiFunction<JsonElement, GraphqlParser.SelectionContext, PublisherBuilder<JsonElement>>> operationHandlers;

    public BaseOperationHandler() {
        this.gsonBuilder = new GsonBuilder()
                .registerTypeAdapter(
                        LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) ->
                                LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                .registerTypeAdapter(
                        LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, type, jsonDeserializationContext) ->
                                LocalDate.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
                .registerTypeAdapter(
                        LocalTime.class,
                        (JsonDeserializer<LocalTime>) (json, type, jsonDeserializationContext) ->
                                LocalTime.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ofPattern("HH:mm:ss"))
                );
        this.operationHandlers = new HashMap<>();
    }

    private BiFunction<JsonElement, GraphqlParser.SelectionContext, PublisherBuilder<JsonElement>> getOperationHandler(String name) {
        return operationHandlers.get(name);
    }

    protected void put(String name, BiFunction<JsonElement, GraphqlParser.SelectionContext, PublisherBuilder<JsonElement>> biFunction) {
        operationHandlers.put(name, biFunction);
    }

    protected Mono<JsonElement> invoke(JsonElement jsonElement, GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return Flux.fromIterable(operationDefinitionContext.selectionSet().selection())
                .flatMap(selectionContext ->
                        getOperationHandler(selectionContext.field().name().getText())
                                .apply(jsonElement.getAsJsonObject().get(selectionContext.field().name().getText()), selectionContext)
                                .map(subJsonElement -> new AbstractMap.SimpleEntry<>(selectionContext.field().name().getText(), subJsonElement))
                                .buildRs()
                )
                .collectList()
                .map(entryList -> {
                            JsonObject jsonObject = new JsonObject();
                            entryList.forEach(entry -> jsonObject.add(entry.getKey(), entry.getValue()));
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

    protected JsonElement toJsonPrimitiveList(Collection<?> collection) {
        if (collection == null) {
            return JsonNull.INSTANCE;
        }
        JsonArray jsonArray = new JsonArray();
        collection.forEach(item -> jsonArray.add(toJsonPrimitive(item)));
        return jsonArray;
    }

    protected JsonElement toJsonPrimitive(Object object) {
        if (object == null) {
            return JsonNull.INSTANCE;
        }
        return gsonBuilder.create().toJsonTree(object);
    }
}
