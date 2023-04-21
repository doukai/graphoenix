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

public class ValueWithVariable implements JsonString, JsonNumber, JsonValue {

    private final JsonValue jsonValue;

    public ValueWithVariable(Object value) {
        if (value instanceof ValueWithVariable) {
            this.jsonValue = ((ValueWithVariable) value).getJsonValue();
        } else if (value instanceof BooleanValue) {
            this.jsonValue = (BooleanValue) value;
        } else if (value instanceof IntValue) {
            this.jsonValue = (IntValue) value;
        } else if (value instanceof FloatValue) {
            this.jsonValue = (FloatValue) value;
        } else if (value instanceof StringValue) {
            this.jsonValue = (StringValue) value;
        } else if (value instanceof EnumValue) {
            this.jsonValue = (EnumValue) value;
        } else if (value instanceof NullValue) {
            this.jsonValue = (NullValue) value;
        } else if (value instanceof ObjectValueWithVariable) {
            this.jsonValue = (ObjectValueWithVariable) value;
        } else if (value instanceof ArrayValueWithVariable) {
            this.jsonValue = (ArrayValueWithVariable) value;
        } else if (value instanceof Variable) {
            this.jsonValue = (Variable) value;
        } else if (value.equals(JsonValue.NULL)) {
            this.jsonValue = new NullValue();
        } else if (value.equals(JsonValue.TRUE)) {
            this.jsonValue = new BooleanValue(true);
        } else if (value.equals(JsonValue.FALSE)) {
            this.jsonValue = new BooleanValue(false);
        } else if (value instanceof JsonNumber) {
            JsonNumber jsonNumber = (JsonNumber) value;
            if (jsonNumber.isIntegral()) {
                this.jsonValue = new IntValue(jsonNumber.intValue());
            } else {
                this.jsonValue = new FloatValue(jsonNumber.doubleValue());
            }
        } else if (value instanceof JsonString) {
            this.jsonValue = new StringValue(((JsonString) value).getString());
        } else if (value instanceof JsonArray) {
            this.jsonValue = new ArrayValueWithVariable((JsonArray) value);
        } else if (value instanceof JsonObject) {
            this.jsonValue = new ObjectValueWithVariable((JsonObject) value);
        } else if (value instanceof GraphqlParser.ValueWithVariableContext) {
            this.jsonValue = toJsonValue((GraphqlParser.ValueWithVariableContext) value);
        } else {
            this.jsonValue = toJsonValue(value);
        }
    }

    private JsonValue toJsonValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
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

    private JsonValue toJsonValue(Object value) {
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
                return toJsonValue(((AnnotationValue) value).getValue());
            }
        } else {
            return new ObjectValueWithVariable(value);
        }
    }

    public JsonValue getJsonValue() {
        return jsonValue;
    }

    public boolean isBoolean() {
        return jsonValue instanceof BooleanValue;
    }

    public boolean isInteger() {
        return jsonValue instanceof IntValue;
    }

    public boolean isFloat() {
        return jsonValue instanceof FloatValue;
    }

    public boolean isString() {
        return jsonValue instanceof StringValue;
    }

    public boolean isNull() {
        return jsonValue instanceof NullValue;
    }

    public boolean isEnum() {
        return jsonValue instanceof EnumValue;
    }

    public boolean isObject() {
        return jsonValue instanceof ObjectValueWithVariable;
    }

    public boolean isArray() {
        return jsonValue instanceof ArrayValueWithVariable;
    }

    public BooleanValue asBoolean() {
        return (BooleanValue) jsonValue;
    }

    public IntValue asInteger() {
        return (IntValue) jsonValue;
    }

    public FloatValue asFloat() {
        return (FloatValue) jsonValue;
    }

    public StringValue asString() {
        return (StringValue) jsonValue;
    }

    public NullValue asNull() {
        return (NullValue) jsonValue;
    }

    public EnumValue asEnum() {
        return (EnumValue) jsonValue;
    }

    public ObjectValueWithVariable asObject() {
        return (ObjectValueWithVariable) jsonValue;
    }

    public ArrayValueWithVariable asArray() {
        return (ArrayValueWithVariable) jsonValue;
    }

    public Boolean getBoolean() {
        return asBoolean().getValue();
    }

    public Integer getInteger() {
        return (Integer) asInteger().getValue();
    }

    public Float getFloat() {
        return (Float) asFloat().getValue();
    }

    @Override
    public String getString() {
        return asString().getValue();
    }

    @Override
    public CharSequence getChars() {
        return asString().getChars();
    }

    public String getEnum() {
        return asEnum().getValue();
    }

    @Override
    public JsonObject asJsonObject() {
        return asObject().asJsonObject();
    }

    @Override
    public JsonArray asJsonArray() {
        return asArray().asJsonArray();
    }

    @Override
    public ValueType getValueType() {
        if (isNull()) {
            return ValueType.NULL;
        } else if (isBoolean()) {
            return asBoolean().getValue() ? ValueType.TRUE : ValueType.FALSE;
        } else if (isString()) {
            return ValueType.STRING;
        } else if (isInteger()) {
            return ValueType.NUMBER;
        } else if (isFloat()) {
            return ValueType.NUMBER;
        } else if (isEnum()) {
            return ValueType.STRING;
        } else if (isObject()) {
            return ValueType.OBJECT;
        } else if (isArray()) {
            return ValueType.ARRAY;
        }
        throw new JsonException("unknown json value type:" + this);
    }

    @Override
    public boolean isIntegral() {
        return isInteger();
    }

    @Override
    public int intValue() {
        return getInteger();
    }

    @Override
    public int intValueExact() {
        return getInteger();
    }

    @Override
    public long longValue() {
        return getInteger();
    }

    @Override
    public long longValueExact() {
        return getInteger();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return BigInteger.valueOf(longValue());
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return BigInteger.valueOf(longValueExact());
    }

    @Override
    public double doubleValue() {
        return getFloat();
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(doubleValue());
    }

    @Override
    public String toString() {
        return jsonValue.toString();
    }
}
