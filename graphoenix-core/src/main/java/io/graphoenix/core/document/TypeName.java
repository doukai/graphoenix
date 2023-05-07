package io.graphoenix.core.document;

public class TypeName implements Type {

    private String name;

    public TypeName() {
    }

    public TypeName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public TypeName setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
