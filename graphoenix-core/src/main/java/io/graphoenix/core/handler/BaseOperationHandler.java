package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static jakarta.json.JsonValue.FALSE;
import static jakarta.json.JsonValue.TRUE;

public abstract class BaseOperationHandler {

    private final Map<String, BiFunction<JsonValue, GraphqlParser.SelectionContext, Flux<JsonValue>>> operationHandlers;
    private final JsonProvider jsonProvider;
    private final Jsonb jsonb;

    public BaseOperationHandler() {
        this.operationHandlers = new HashMap<>();
        this.jsonProvider = BeanContext.get(JsonProvider.class);
        this.jsonb = BeanContext.get(Jsonb.class);
    }

    private BiFunction<JsonValue, GraphqlParser.SelectionContext, Flux<JsonValue>> getOperationHandler(String name) {
        return operationHandlers.get(name);
    }

    protected void put(String name, BiFunction<JsonValue, GraphqlParser.SelectionContext, Flux<JsonValue>> biFunction) {
        operationHandlers.put(name, biFunction);
    }

    protected Mono<JsonValue> invoke(JsonValue jsonValue, GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return Flux.fromIterable(operationDefinitionContext.selectionSet().selection())
                .flatMap(selectionContext ->
                        getOperationHandler(selectionContext.field().name().getText())
                                .apply(jsonValue.asJsonObject().get(selectionContext.field().name().getText()), selectionContext)
                                .map(subJsonValue -> new AbstractMap.SimpleEntry<>(selectionContext.field().name().getText(), subJsonValue))
                )
                .collectList()
                .map(list -> list.stream().collect(JsonCollectors.toJsonObject()));
    }

    protected <T> T getArgument(GraphqlParser.SelectionContext selectionContext, String name, Class<T> beanClass) {
        return selectionContext.field().arguments().argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals(name))
                .findFirst()
                .map(argumentContext -> jsonb.fromJson(argumentContext.valueWithVariable().getText(), beanClass))
                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SELECTION_ARGUMENT_NOT_EXIST.bind(name, selectionContext.field().name().getText())));
    }

    protected JsonValue toJsonValueList(Collection<?> collection) {
        if (collection == null) {
            return JsonValue.NULL;
        }
        return jsonProvider.createArrayBuilder(collection).build();
    }

    protected JsonValue toJsonValue(String value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(Integer value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(Long value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(Double value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(BigDecimal value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(BigInteger value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(Boolean value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        return value ? TRUE : FALSE;
    }
}
