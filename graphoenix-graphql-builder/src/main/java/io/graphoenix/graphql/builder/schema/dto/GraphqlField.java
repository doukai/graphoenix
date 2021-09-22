package io.graphoenix.graphql.builder.schema.dto;

public class GraphqlField {

    private String name;

    private String typeName;

    private boolean isNonNull;

    private boolean isList;

    private boolean isNonNullList;

    private boolean isObject;

    private boolean isBoolean;

    private boolean isLast;

    public GraphqlField(String name, String typeName, boolean isNonNull, boolean isList, boolean isNonNullList, boolean isObject) {
        this.setName(name);
        this.setTypeName(typeName);
        this.setNonNull(isNonNull);
        this.setList(isList);
        this.setNonNullList(isNonNullList);
        this.setObject(isObject);
    }

    public String getName() {
        return name;
    }

    public GraphqlField setName(String name) {
        this.name = name;
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public GraphqlField setTypeName(String typeName) {
        this.typeName = typeName;
        if (this.typeName.equals("Boolean")) {
            this.isBoolean = true;
        }
        return this;
    }

    public boolean isNonNull() {
        return isNonNull;
    }

    public void setNonNull(boolean nonNull) {
        isNonNull = nonNull;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public boolean isNonNullList() {
        return isNonNullList;
    }

    public void setNonNullList(boolean nonNullList) {
        isNonNullList = nonNullList;
    }

    public boolean isObject() {
        return isObject;
    }

    public GraphqlField setObject(boolean object) {
        isObject = object;
        return this;
    }

    public boolean isLast() {
        return isLast;
    }

    public GraphqlField setLast(boolean last) {
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
