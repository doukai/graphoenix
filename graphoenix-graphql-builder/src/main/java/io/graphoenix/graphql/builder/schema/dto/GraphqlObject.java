package io.graphoenix.graphql.builder.schema.dto;

import java.util.List;

public class GraphqlObject {

    private String schemaFieldName;

    private String name;

    private List<GraphqlField> fields;

    private List<GraphqlDirective> directives;

    public GraphqlObject(String name, List<GraphqlField> fields, List<GraphqlDirective> directives) {
        this.setName(name);
        this.setFields(fields);
        this.setDirectives(directives);
    }

    public GraphqlObject(String name, List<GraphqlField> fields) {
        this.setName(name);
        this.setFields(fields);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.schemaFieldName = this.name.toLowerCase();
    }

    public List<GraphqlField> getFields() {
        return fields;
    }

    public void setFields(List<GraphqlField> fields) {
        this.fields = fields;
    }

    public List<GraphqlDirective> getDirectives() {
        return directives;
    }

    public GraphqlObject setDirectives(List<GraphqlDirective> directives) {
        this.directives = directives;
        return this;
    }

    public String getSchemaFieldName() {
        return schemaFieldName;
    }

    public void setSchemaFieldName(String schemaFieldName) {
        this.schemaFieldName = schemaFieldName;
    }
}
