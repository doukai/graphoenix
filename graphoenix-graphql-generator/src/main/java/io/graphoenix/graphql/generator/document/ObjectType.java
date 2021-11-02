package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class ObjectType {

    private final STGroup stGroupFile = new STGroupFile("stg/document/ObjectType.stg");

    private String name;
    private List<String> interfaces;
    private List<String> directives;
    private List<Field> fields;
    private String description;

    public String getName() {
        return name;
    }

    public ObjectType setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public ObjectType setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public ObjectType setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public List<Field> getFields() {
        return fields;
    }

    public ObjectType setFields(List<Field> fields) {
        this.fields = fields;
        return this;
    }

    public ObjectType addFields(List<Field> fields) {
        if (this.fields == null) {
            this.fields = fields;
        } else {
            this.fields.addAll(fields);
        }
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ObjectType setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("objectTypeDefinition");
        st.add("objectType", this);
        return st.render();
    }
}
