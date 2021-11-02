package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class InputObjectType {

    private final STGroup stGroupFile = new STGroupFile("stg/document/InputObjectType.stg");

    private String name;
    private List<String> directives;
    private List<InputValue> inputValues;
    private String description;

    public String getName() {
        return name;
    }

    public InputObjectType setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public InputObjectType setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public List<InputValue> getInputValues() {
        return inputValues;
    }

    public InputObjectType setInputValues(List<InputValue> inputValues) {
        this.inputValues = inputValues;
        return this;
    }

    public InputObjectType addInputValue(InputValue inputValue) {
        if (this.inputValues == null) {
            this.inputValues = new ArrayList<>();
        }
        this.inputValues.add(inputValue);
        return this;
    }

    public InputObjectType addInputValues(List<InputValue> inputValues) {
        if (this.inputValues == null) {
            this.inputValues = inputValues;
        } else {
            this.inputValues.addAll(inputValues);
        }
        return this;
    }

    public String getDescription() {
        return description;
    }

    public InputObjectType setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("inputObjectTypeDefinition");
        st.add("inputObjectType", this);
        return st.render();
    }
}
