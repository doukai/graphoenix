package io.graphoenix.greator;

import java.util.List;

public class GraphqlObject {

    private String name;

    private List<GraphqlField> fields;

    public GraphqlObject(String name, List<GraphqlField> fields) {
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GraphqlField> getFields() {
        return fields;
    }

    public void setFields(List<GraphqlField> fields) {
        this.fields = fields;
    }
}
