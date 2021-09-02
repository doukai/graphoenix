package io.graphoenix.graphql.builder.dto;

public class GraphqlField {

    private String name;

    private String typeName;

    private boolean isObject;

    public GraphqlField(String name, String typeName, boolean isObject) {
        this.name = name;
        this.typeName = typeName;
        this.isObject = isObject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public boolean isObject() {
        return isObject;
    }

    public void setObject(boolean object) {
        isObject = object;
    }
}
