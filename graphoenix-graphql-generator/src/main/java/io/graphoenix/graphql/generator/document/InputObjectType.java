package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.HashSet;
import java.util.Set;

public class InputObjectType {

    private String name;
    private Set<String> directives;
    private Set<InputValue> inputValues;
    private String description;

    public String getName() {
        return name;
    }

    public InputObjectType setName(String name) {
        this.name = name;
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public InputObjectType setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<InputValue> getInputValues() {
        return inputValues;
    }

    public InputObjectType setInputValues(Set<InputValue> inputValues) {
        this.inputValues = inputValues;
        return this;
    }

    public InputObjectType addInputValue(InputValue inputValue) {
        if (this.inputValues == null) {
            this.inputValues = new HashSet<>();
        }
        this.inputValues.add(inputValue);
        return this;
    }

    public InputObjectType addInputValues(Set<InputValue> inputValues) {
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/InputObjectType.stg");
        ST st = stGroupFile.getInstanceOf("inputObjectTypeDefinition");
        st.add("inputObjectType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
