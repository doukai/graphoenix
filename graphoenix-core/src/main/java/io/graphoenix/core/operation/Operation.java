package io.graphoenix.core.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Operation {

    private String operationType;
    private String name;
    private Set<VariableDefinition> variableDefinitions;
    private Set<String> directives;
    private Set<Field> fields;

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

    public Set<VariableDefinition> getVariableDefinitions() {
        return variableDefinitions;
    }

    public Operation setVariableDefinitions(Set<VariableDefinition> variableDefinitions) {
        this.variableDefinitions = variableDefinitions;
        return this;
    }

    public Operation addVariableDefinition(VariableDefinition variableDefinition) {
        if (this.variableDefinitions == null) {
            this.variableDefinitions = new LinkedHashSet<>();
        }
        this.variableDefinitions.add(variableDefinition);
        return this;
    }

    public Operation addVariableDefinitions(Stream<VariableDefinition> variableDefinitionStream) {
        if (this.variableDefinitions == null) {
            this.variableDefinitions = new LinkedHashSet<>();
        }
        this.variableDefinitions.addAll(variableDefinitionStream.collect(Collectors.toList()));
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public Operation setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public Operation setFields(Set<Field> fields) {
        this.fields = fields;
        return this;
    }

    public Operation addField(Field field) {
        if (this.fields == null) {
            this.fields = new LinkedHashSet<>();
        }
        this.fields.add(field);
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Operation.stg");
        ST st = stGroupFile.getInstanceOf("operationDefinition");
        st.add("operation", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
