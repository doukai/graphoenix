package io.graphoenix.core.document;

public class ListType implements Type {

    private Type type;

    public ListType() {
    }

    public ListType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public ListType setType(Type type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public String toString() {
        return "[" + type + "]";
    }
}
