package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class InterfaceType {

    private String name;
    private List<String> interfaces;
    private List<String> directives;
    private List<Field> fields;
    private String description;

    public String getName() {
        return name;
    }

    public InterfaceType setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public InterfaceType setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public InterfaceType setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public List<Field> getFields() {
        return fields;
    }

    public InterfaceType setFields(List<Field> fields) {
        this.fields = fields;
        return this;
    }

    public InterfaceType addFields(List<Field> fields) {
        if (this.fields == null) {
            this.fields = fields;
        } else {
            if (fields != null) {
                this.fields.addAll(fields);
            }
        }
        return this;
    }

    public String getDescription() {
        return description;
    }

    public InterfaceType setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/document/InterfaceType.stg").getInstanceOf("interfaceTypeDefinition");
        st.add("interfaceType", this);
        return st.render();
    }
}
