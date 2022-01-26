package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class InterfaceType {

    private String name;
    private Set<String> interfaces;
    private Set<String> directives;
    private Set<Field> fields;
    private String description;

    public String getName() {
        return name;
    }

    public InterfaceType setName(String name) {
        this.name = name;
        return this;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }

    public InterfaceType setInterfaces(Set<String> interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public InterfaceType setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public InterfaceType setFields(Set<Field> fields) {
        this.fields = fields;
        return this;
    }

    public InterfaceType addFields(Set<Field> fields) {
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/InterfaceType.stg");
        ST st = stGroupFile.getInstanceOf("interfaceTypeDefinition");
        st.add("interfaceType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
