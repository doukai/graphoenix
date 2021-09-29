package io.graphoenix.graphql.builder.schema.dto;

import java.util.List;

public class GraphqlField {

    private String name;

    private String typeName;

    private GraphqlObject type;

    private List<GraphqlDirective> directives;

    private boolean isNonNull;

    private boolean isList;

    private boolean isNonNullList;

    private boolean isObject;

    private boolean isBoolean;

    private boolean isLast;

    public GraphqlField(String name, List<GraphqlDirective> directives) {
        this.setName(name);
        this.setDirectives(directives);
    }

    public GraphqlField(String name, String typeName) {
        this.setName(name);
        this.setTypeName(typeName);
    }

    public GraphqlField(String name, String typeName, List<GraphqlDirective> directives, boolean isNonNull, boolean isList, boolean isNonNullList, boolean isObject) {
        this.setName(name);
        this.setDirectives(directives);
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

    public GraphqlObject getType() {
        return type;
    }

    public GraphqlField setType(GraphqlObject type) {
        this.type = type;
        return this;
    }

    public List<GraphqlDirective> getDirectives() {
        return directives;
    }

    public GraphqlField setDirectives(List<GraphqlDirective> directives) {
        this.directives = directives;
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
