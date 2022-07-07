package io.graphoenix.core.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class __Type {

    private __TypeKind kind;

    private String name;

    private String description;

    private Set<__Field> fields;

    private Set<__Type> interfaces;

    private Set<__Type> possibleTypes;

    private Set<__EnumValue> enumValues;

    private Set<__InputValue> inputFields;

    private __Type ofType;

    public __TypeKind getKind() {
        return kind;
    }

    public void setKind(__TypeKind kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<__Field> getFields() {
        return fields;
    }

    public void setFields(Set<__Field> fields) {
        this.fields = fields;
    }

    public Set<__Type> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Set<__Type> interfaces) {
        this.interfaces = interfaces;
    }

    public Set<__Type> getPossibleTypes() {
        return possibleTypes;
    }

    public void setPossibleTypes(Set<__Type> possibleTypes) {
        this.possibleTypes = possibleTypes;
    }

    public Set<__EnumValue> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(Set<__EnumValue> enumValues) {
        this.enumValues = enumValues;
    }

    public Set<__InputValue> getInputFields() {
        return inputFields;
    }

    public void setInputFields(Set<__InputValue> inputFields) {
        this.inputFields = inputFields;
    }

    public __Type getOfType() {
        return ofType;
    }

    public void setOfType(__Type ofType) {
        this.ofType = ofType;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/introspection/__Type.stg");
        ST st = stGroupFile.getInstanceOf("__typeDefinition");
        st.add("__type", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}