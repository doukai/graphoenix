package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            this.interfaces = new LinkedHashSet<>();
        }
        this.interfaces.add(interfaceType);
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public ObjectType setStringDirectives(Set<String> directives) {
        if (directives != null) {
            this.directives = directives.stream().map(directive -> !directive.startsWith("@") ? "@".concat(directive) : directive).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this;
    }

    public ObjectType setDirectives(Set<Directive> directives) {
        if (directives != null) {
            this.directives = directives.stream().map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this;
    }

    public ObjectType addStringDirective(String directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        if (!directive.startsWith("@")) {
            directive = "@".concat(directive);
        }
        this.directives.add(directive);
        return this;
    }

    public ObjectType addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive.toString());
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
            this.fields = new LinkedHashSet<>();
        }
        this.fields.addAll(fields);
        return this;
    }

    public ObjectType addField(Field field) {
        if (this.fields == null) {
            this.fields = new LinkedHashSet<>();
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
