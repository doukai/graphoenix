package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.RuleContext;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class ObjectType {

    private String name;
    private Set<String> interfaces;
    private Set<String> directives;
    private Set<Field> fields;
    private String description;

    public ObjectType() {
    }

    public ObjectType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        this.name = objectTypeDefinitionContext.name().getText();
        if (objectTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(objectTypeDefinitionContext.description().StringValue());
        }
        if (objectTypeDefinitionContext.implementsInterfaces() != null) {
            this.interfaces = objectTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toSet());
        }
        if (objectTypeDefinitionContext.directives() != null) {
            this.directives = objectTypeDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toSet());
        }
        if (objectTypeDefinitionContext.fieldsDefinition() != null) {
            this.fields = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(Field::new).collect(Collectors.toSet());
        }
    }

    public static ObjectType merge(GraphqlParser.ObjectTypeDefinitionContext... objectTypeDefinitionContexts) {
        return merge(Stream.of(objectTypeDefinitionContexts).map(ObjectType::new).toArray(ObjectType[]::new));
    }

    public static ObjectType merge(ObjectType... objectTypes) {
        ObjectType objectType = new ObjectType();
        objectType.name = objectTypes[0].getName();
        objectType.description = objectTypes[0].getDescription();
        objectType.interfaces = Stream.of(objectTypes).flatMap(item -> Stream.ofNullable(item.getInterfaces()).flatMap(Collection::stream).distinct()).collect(Collectors.toSet());
        objectType.directives = Stream.of(objectTypes).flatMap(item -> Stream.ofNullable(item.getDirectives()).flatMap(Collection::stream).distinct()).collect(Collectors.toSet());
        for (ObjectType item : objectTypes) {
            for (Field itemField : item.getFields()) {
                if (objectType.fields.stream().noneMatch(field -> field.getName().equals(itemField.getName()))) {
                    objectType.fields.add(itemField);
                }
            }
        }
        return objectType;
    }

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
