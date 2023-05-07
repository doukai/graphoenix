package io.graphoenix.core.document;

public class NonNullType implements Type {

    private Type type;

    public NonNullType() {
    }

    public NonNullType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public NonNullType setType(Type type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean isNonNull() {
        return true;
    }

    @Override
    public String toString() {
        return type + "!";
    }
}
