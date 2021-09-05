package io.graphoenix.graphql.builder.dto;

import java.util.List;

public class GraphqlObjectDto {

    private String schemaFieldName;

    private String name;

    private List<GraphqlFieldDto> fields;

    public GraphqlObjectDto(String name, List<GraphqlFieldDto> fields) {
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

    public List<GraphqlFieldDto> getFields() {
        return fields;
    }

    public void setFields(List<GraphqlFieldDto> fields) {
        this.fields = fields;
    }

    public String getSchemaFieldName() {
        return schemaFieldName;
    }

    public void setSchemaFieldName(String schemaFieldName) {
        this.schemaFieldName = schemaFieldName;
    }
}
