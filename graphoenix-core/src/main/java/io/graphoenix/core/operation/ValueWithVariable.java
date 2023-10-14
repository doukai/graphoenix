package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import jakarta.json.*;
import jakarta.json.stream.JsonCollectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.IntStream;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_VALUE;
import static javax.lang.model.element.ElementKind.ENUM_CONSTANT;
import static javax.lang.model.element.ElementKind.PARAMETER;

public interface ValueWithVariable extends JsonValue {

    static <T extends ValueWithVariable> ValueWithVariable of(T value) {
        return value;
    }

    static <T extends JsonValue> ValueWithVariable of(T value) {
        if (value instanceof ValueWithVariable) {
            return (ValueWithVariable) value;
        } else if (value.equals(JsonValue.NULL)) {
            return new NullValue();
        } else if (value.equals(JsonValue.TRUE)) {
            return new BooleanValue(true);
        } else if (value.equals(JsonValue.FALSE)) {
            return new BooleanValue(false);
        } else if (value instanceof JsonNumber) {
            JsonNumber jsonNumber = (JsonNumber) value;
            if (jsonNumber.isIntegral()) {
                return new IntValue(jsonNumber.intValue());
            } else {
                return new FloatValue(jsonNumber.doubleValue());
            }
        } else if (value instanceof JsonString) {
            return new StringValue(((JsonString) value).getString());
        } else if (value instanceof JsonArray) {
            return new ArrayValueWithVariable((JsonArray) value);
        } else if (value instanceof JsonObject) {
            return new ObjectValueWithVariable((JsonObject) value);
        }
        throw new GraphQLErrors(UNSUPPORTED_VALUE.bind(value.toString()));
    }

    static ValueWithVariable of(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.NullValue() != null) {
            return new NullValue();
        } else if (valueWithVariableContext.variable() != null) {
            return new Variable(valueWithVariableContext.variable().name().getText());
        } else if (valueWithVariableContext.BooleanValue() != null) {
            return new BooleanValue(valueWithVariableContext.BooleanValue());
        } else if (valueWithVariableContext.IntValue() != null) {
            return new IntValue(valueWithVariableContext.IntValue());
        } else if (valueWithVariableContext.FloatValue() != null) {
            return new FloatValue(valueWithVariableContext.FloatValue());
        } else if (valueWithVariableContext.StringValue() != null) {
            return new StringValue(valueWithVariableContext.StringValue());
        } else if (valueWithVariableContext.enumValue() != null) {
            return new EnumValue(valueWithVariableContext.enumValue());
        } else if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return new ArrayValueWithVariable(valueWithVariableContext.arrayValueWithVariable());
        } else if (valueWithVariableContext.objectValueWithVariable() != null) {
            return new ObjectValueWithVariable(valueWithVariableContext.objectValueWithVariable());
        }
        throw new GraphQLErrors(UNSUPPORTED_VALUE.bind(valueWithVariableContext.getText()));
    }

    static ValueWithVariable of(Object value) {
        if (value == null) {
            return new NullValue();
        } else if (value instanceof ValueWithVariable) {
            return (ValueWithVariable) value;
        } else if (value instanceof JsonValue) {
            return of((JsonValue) value);
        } else if (value instanceof GraphqlParser.ValueWithVariableContext) {
            return of((GraphqlParser.ValueWithVariableContext) value);
        } else if (value instanceof VariableElement) {
            if (((VariableElement) value).getKind().equals(PARAMETER)) {
                return new Variable(((VariableElement) value).getSimpleName().toString());
            } else if (((VariableElement) value).getKind().equals(ENUM_CONSTANT)) {
                return new EnumValue(value.toString());
            } else {
                return of(((VariableElement) value).getConstantValue());
            }
        } else if (value instanceof Boolean) {
            return new BooleanValue((Boolean) value);
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte || value instanceof BigInteger) {
            return new IntValue((Number) value);
        } else if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            return new FloatValue((Number) value);
        } else if (value instanceof String) {
            return new StringValue((String) value);
        } else if (value instanceof LocalDate) {
            return new StringValue((LocalDate) value);
        } else if (value instanceof LocalTime) {
            return new StringValue((LocalTime) value);
        } else if (value instanceof LocalDateTime) {
            return new StringValue((LocalDateTime) value);
        } else if (value instanceof Character) {
            return new StringValue((Character) value);
        } else if (value instanceof Enum<?>) {
            return new EnumValue((Enum<?>) value);
        } else if (value instanceof Arrays) {
            return new ArrayValueWithVariable((Arrays) value);
        } else if (value instanceof Collection<?>) {
            return new ArrayValueWithVariable((Collection<?>) value);
        } else if (value instanceof Map<?, ?>) {
            return new ObjectValueWithVariable((Map<?, ?>) value);
        } else if (value instanceof AnnotationMirror) {
            return new ObjectValueWithVariable((AnnotationMirror) value);
        } else if (value instanceof AnnotationValue) {
            if (value.getClass().getSimpleName().equals("Enum")) {
                return new EnumValue((AnnotationValue) value);
            } else {
                return of(((AnnotationValue) value).getValue());
            }
        } else {
            return new ObjectValueWithVariable(value);
        }
    }

    default boolean isBoolean() {
        return false;
    }

    default boolean isInt() {
        return false;
    }

    default boolean isFloat() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isNull() {
        return false;
    }

    default boolean isEnum() {
        return false;
    }

    default boolean isObject() {
        return false;
    }

    default boolean isArray() {
        return false;
    }

    default boolean isVariable() {
        return false;
    }

    static JsonObject updateJsonObject(JsonObject original, JsonObject jsonObject) {
        return jsonObject.entrySet().stream()
                .filter(entry ->
                        !entry.getValue().getValueType().equals(ValueType.NULL) ||
                                entry.getValue().getValueType().equals(ValueType.NULL) && original != null && original.containsKey(entry.getKey())
                )
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), updateJsonValue(original != null ? original.get(entry.getKey()) : null, entry.getValue())))
                .collect(JsonCollectors.toJsonObject());
    }

    static JsonValue updateJsonValue(JsonValue original, JsonValue jsonValue) {
        if (jsonValue.getValueType().equals(ValueType.OBJECT)) {
            return updateJsonObject(original != null ? original.asJsonObject() : null, jsonValue.asJsonObject());
        } else if (jsonValue.getValueType().equals(ValueType.ARRAY)) {
            return IntStream.range(0, jsonValue.asJsonArray().size())
                    .mapToObj(index -> updateJsonValue(original != null ? original.asJsonArray().get(index) : null, jsonValue.asJsonArray().get(index)))
                    .collect(JsonCollectors.toJsonArray());
        } else {
            return jsonValue;
        }
    }
}
