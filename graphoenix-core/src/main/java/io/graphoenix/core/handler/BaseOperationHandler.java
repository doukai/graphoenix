package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.vavr.Function3;
import jakarta.json.JsonValue;
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

import static jakarta.json.JsonValue.FALSE;
import static jakarta.json.JsonValue.NULL;
import static jakarta.json.JsonValue.TRUE;

public abstract class BaseOperationHandler {

    private final Map<String, Function3<JsonValue, GraphqlParser.SelectionContext, QueryDataLoader, Mono<JsonValue>>> operationHandlers;
    private final JsonProvider jsonProvider;

    public BaseOperationHandler() {
        this.operationHandlers = new HashMap<>();
        this.jsonProvider = BeanContext.get(JsonProvider.class);
    }

    private Function3<JsonValue, GraphqlParser.SelectionContext, QueryDataLoader, Mono<JsonValue>> getOperationHandler(String name) {
        return operationHandlers.get(name);
    }

    protected void put(String name, Function3<JsonValue, GraphqlParser.SelectionContext, QueryDataLoader, Mono<JsonValue>> function3) {
        operationHandlers.put(name, function3);
    }

    protected Mono<JsonValue> invoke(JsonValue jsonValue, GraphqlParser.OperationDefinitionContext operationDefinitionContext, QueryDataLoader loader) {
        return Flux.fromIterable(operationDefinitionContext.selectionSet().selection())
                .flatMap(selectionContext -> {
                            String selectionName = selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText();
                            JsonValue fieldValue = jsonValue.asJsonObject().get(selectionName);
                            if (fieldValue == null || fieldValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                                return Mono.just(new AbstractMap.SimpleEntry<>(selectionName, fieldValue));
                            } else {
                                return getOperationHandler(selectionContext.field().name().getText())
                                        .apply(fieldValue, selectionContext, loader)
                                        .map(subJsonValue -> new AbstractMap.SimpleEntry<>(selectionName, subJsonValue));
                            }
                        }
                )
                .collectList()
                .map(list -> list.stream().collect(JsonCollectors.toJsonObject()));
    }

    protected JsonValue toJsonValueList(Collection<?> collection) {
        if (collection == null) {
            return NULL;
        }
        return jsonProvider.createArrayBuilder(collection).build();
    }

    protected JsonValue toJsonValue(String value) {
        if (value == null) {
            return NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(Integer value) {
        if (value == null) {
            return NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(Long value) {
        if (value == null) {
            return NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(Double value) {
        if (value == null) {
            return NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(BigDecimal value) {
        if (value == null) {
            return NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(BigInteger value) {
        if (value == null) {
            return NULL;
        }
        return jsonProvider.createValue(value);
    }

    protected JsonValue toJsonValue(Boolean value) {
        if (value == null) {
            return NULL;
        }
        return value ? TRUE : FALSE;
    }
}
