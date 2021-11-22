package io.graphoenix.graphql.generator.operation;

import javax.lang.model.element.VariableElement;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

public class ValueWithVariable {

    private final Object valueWithVariable;

    public ValueWithVariable(Object value) {
        if (value == null) {
            valueWithVariable = new NullValue();
        } else if (value instanceof VariableElement) {
            valueWithVariable = new Variable(((VariableElement) value).getSimpleName().toString());
        } else if (value instanceof Boolean) {
            valueWithVariable = new BooleanValue((Boolean) value);
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof BigInteger) {
            valueWithVariable = new IntValue((Number) value);
        } else if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            valueWithVariable = new FloatValue((Number) value);
        } else if (value instanceof String) {
            valueWithVariable = new StringValue((String) value);
        } else if (value instanceof Enum<?>) {
            valueWithVariable = new EnumValue((Enum<?>) value);
        } else if (value instanceof Collection<?>) {
            valueWithVariable = new ArrayValueWithVariable((Collection<?>) value);
        } else {
            valueWithVariable = new ObjectValueWithVariable(value);
        }

    }

    @Override
    public String toString() {
        return valueWithVariable.toString();
    }
}
