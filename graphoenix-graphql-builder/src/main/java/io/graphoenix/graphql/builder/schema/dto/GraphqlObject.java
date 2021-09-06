package io.graphoenix.graphql.builder.schema.dto;

import java.util.List;

public class GraphqlObject {

    private String schemaFieldName;

    private String name;

    private List<GraphqlField> fields;

    public GraphqlObject(String name, List<GraphqlField> fields) {
        this.name = name;
        this.schemaFieldName = this.name.toLowerCase();
        this.fields = fields;
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

    public String getSchemaFieldName() {
        return schemaFieldName;
    }

    public void setSchemaFieldName(String schemaFieldName) {
        this.schemaFieldName = schemaFieldName;
    }
}
