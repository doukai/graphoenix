package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObjectType {

    private String name;
    private Set<String> interfaces;
    private Set<String> directives;
    private Set<Field> fields;
    private String description;

    public String getName() {
        return name;
    }

    public ObjectType setName(String name) {
        this.name = name;
        return this;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }

    public ObjectType setInterfaces(Set<String> interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    public ObjectType addInterface(String interfaceType) {
        if (this.interfaces == null) {
            this.interfaces = new HashSet<>();
        }
        this.interfaces.add(interfaceType);
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public ObjectType setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public ObjectType setFields(Set<Field> fields) {
        this.fields = fields;
        return this;
    }

    public ObjectType addFields(List<Field> fields) {
        if (this.fields == null) {
            this.fields = new HashSet<>();
        }
        this.fields.addAll(fields);
        return this;
    }

    public ObjectType addField(Field field) {
        if (this.fields == null) {
            this.fields = new HashSet<>();
        }
        this.fields.add(field);
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/ObjectType.stg");
        ST st = stGroupFile.getInstanceOf("objectTypeDefinition");
        st.add("objectType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
