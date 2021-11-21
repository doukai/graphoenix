package io.graphoenix.graphql.generator.operation;

import io.graphoenix.graphql.generator.document.InputValue;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class Operation {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/Operation.stg");

    private String operationType;
    private String name;
    private List<InputValue> variableDefinitions;
    private List<String> directives;
    private List<Field> fields;

    public String getOperationType() {
        return operationType;
    }

    public Operation setOperationType(String operationType) {
        this.operationType = operationType;
        return this;
    }

    public String getName() {
        return name;
    }

    public Operation setName(String name) {
        this.name = name;
        return this;
    }

    public List<InputValue> getVariableDefinitions() {
        return variableDefinitions;
    }

    public Operation setVariableDefinitions(List<InputValue> variableDefinitions) {
        this.variableDefinitions = variableDefinitions;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public Operation setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public List<Field> getFields() {
        return fields;
    }

    public Operation setFields(List<Field> fields) {
        this.fields = fields;
        return this;
    }

    public Operation addField(Field field) {
        if (this.fields == null) {
            this.fields = new ArrayList<>();
        }
        this.fields.add(field);
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("operationDefinition");
        st.add("operation", this);
        return st.render();
    }
}
