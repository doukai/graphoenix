package io.graphoenix.graphql.builder.dto;

public class GraphqlFieldDto {

    private String name;

    private String typeName;

    private boolean isObject;

    private boolean isBoolean;

    private boolean isLast;

    public GraphqlFieldDto(String name, String typeName, boolean isObject) {
        this.name = name;
        this.typeName = typeName;
        if (this.typeName.equals("Boolean")) {
            this.isBoolean = true;
        }
        this.isObject = isObject;
    }

    public String getName() {
        return name;
    }

    public GraphqlFieldDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public GraphqlFieldDto setTypeName(String typeName) {
        this.typeName = typeName;
        if (this.typeName.equals("Boolean")) {
            this.isBoolean = true;
        }
        return this;
    }

    public boolean isObject() {
        return isObject;
    }

    public GraphqlFieldDto setObject(boolean object) {
        isObject = object;
        return this;
    }

    public boolean isLast() {
        return isLast;
    }

    public GraphqlFieldDto setLast(boolean last) {
        isLast = last;
        return this;
    }

    public boolean isBoolean() {
        return isBoolean;
    }

    public void setBoolean(boolean aBoolean) {
        isBoolean = aBoolean;
    }
}
