package io.graphoenix.graphql.generator.operation;

public class ObjectValue extends ValueWithVariable {
    @Override
    public String getValueWithVariable() {
        return this.toString();
    }
}
