package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;

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

public class ValueWithVariable {

    private final Object valueWithVariable;

    public ValueWithVariable(Object value) {
        this.valueWithVariable = getValueWithVariable(value);
    }

    public ValueWithVariable(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        this.valueWithVariable = getValueWithVariable(valueWithVariableContext);
    }

    private Object getValueWithVariable(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
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

    private Object getValueWithVariable(Object value) {
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
        } else if (value instanceof BooleanValue) {
            return value;
        } else if (value instanceof IntValue) {
            return value;
        } else if (value instanceof FloatValue) {
            return value;
        } else if (value instanceof StringValue) {
            return value;
        } else if (value instanceof NullValue) {
            return value;
        } else if (value instanceof EnumValue) {
            return value;
        } else if (value instanceof ObjectValueWithVariable) {
            return value;
        } else if (value instanceof ArrayValueWithVariable) {
            return value;
        } else if (value instanceof Variable) {
            return value;
        } else if (value instanceof AnnotationValue) {
            if (value.getClass().getSimpleName().equals("Enum")) {
                return new EnumValue((AnnotationValue) value);
            } else {
                return getValueWithVariable(((AnnotationValue) value).getValue());
            }
        } else {
            return new ObjectValueWithVariable(value);
        }
    }

    public boolean isBoolean() {
        return valueWithVariable instanceof BooleanValue;
    }

    public boolean isInteger() {
        return valueWithVariable instanceof IntValue;
    }

    public boolean isFloat() {
        return valueWithVariable instanceof FloatValue;
    }

    public boolean isString() {
        return valueWithVariable instanceof StringValue;
    }

    public boolean isNull() {
        return valueWithVariable instanceof NullValue;
    }

    public boolean isEnum() {
        return valueWithVariable instanceof EnumValue;
    }

    public boolean isObject() {
        return valueWithVariable instanceof ObjectValueWithVariable;
    }

    public boolean isArray() {
        return valueWithVariable instanceof ArrayValueWithVariable;
    }

    public BooleanValue asBoolean() {
        return (BooleanValue) valueWithVariable;
    }

    public IntValue asInteger() {
        return (IntValue) valueWithVariable;
    }

    public FloatValue asFloat() {
        return (FloatValue) valueWithVariable;
    }

    public StringValue asString() {
        return (StringValue) valueWithVariable;
    }

    public NullValue asNull() {
        return (NullValue) valueWithVariable;
    }

    public EnumValue asEnum() {
        return (EnumValue) valueWithVariable;
    }

    public ObjectValueWithVariable asObject() {
        return (ObjectValueWithVariable) valueWithVariable;
    }

    public ArrayValueWithVariable asArray() {
        return (ArrayValueWithVariable) valueWithVariable;
    }

    public Boolean getBoolean() {
        return ((BooleanValue) valueWithVariable).getValue();
    }

    public Integer getInteger() {
        return (Integer) ((IntValue) valueWithVariable).getValue();
    }

    public Float getFloat() {
        return (Float) ((FloatValue) valueWithVariable).getValue();
    }

    public String getString() {
        return ((StringValue) valueWithVariable).getValue();
    }

    public String getEnum() {
        return ((EnumValue) valueWithVariable).getValue();
    }

    @Override
    public String toString() {
        return valueWithVariable.toString();
    }
}
