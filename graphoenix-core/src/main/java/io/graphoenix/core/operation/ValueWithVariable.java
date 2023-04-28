package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import jakarta.json.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_VALUE;

public interface ValueWithVariable extends JsonValue {

    static <T extends ValueWithVariable> ValueWithVariable of(T value) {
        return value;
    }

    static <T extends JsonValue> ValueWithVariable of(T value) {
        if (value.equals(JsonValue.NULL)) {
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
        } else if (value instanceof VariableElement) {
            return new Variable(((VariableElement) value).getSimpleName().toString());
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

    String toString();
}
