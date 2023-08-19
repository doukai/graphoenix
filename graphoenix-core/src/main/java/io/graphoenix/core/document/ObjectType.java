package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.operation.Directive;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class ObjectType {

    private String name;
    private Collection<String> interfaces;
    private Collection<Directive> directives;
    private Collection<Field> fields;
    private String description;

    public ObjectType() {
    }

    public ObjectType(String name) {
        this.name = name;
    }

    public ObjectType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        this.name = objectTypeDefinitionContext.name().getText();
        if (objectTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(objectTypeDefinitionContext.description().StringValue());
        }
        if (objectTypeDefinitionContext.implementsInterfaces() != null) {
            this.interfaces = getInterfaces(objectTypeDefinitionContext.implementsInterfaces()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (objectTypeDefinitionContext.directives() != null) {
            this.directives = objectTypeDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (objectTypeDefinitionContext.fieldsDefinition() != null) {
            this.fields = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(Field::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private Stream<String> getInterfaces(GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext) {
        return Stream.concat(
                Stream.ofNullable(implementsInterfacesContext.typeName())
                        .flatMap(Collection::stream)
                        .map(typeNameContext -> typeNameContext.name().getText()),
                Stream.ofNullable(implementsInterfacesContext.implementsInterfaces())
                        .flatMap(this::getInterfaces)
        );
    }

    public static ObjectType merge(GraphqlParser.ObjectTypeDefinitionContext... objectTypeDefinitionContexts) {
        return merge(Stream.of(objectTypeDefinitionContexts).map(ObjectType::new).toArray(ObjectType[]::new));
    }

    public static ObjectType merge(ObjectType... objectTypes) {
        ObjectType objectType = new ObjectType();
        objectType.name = objectTypes[0].getName();
        objectType.description = objectTypes[0].getDescription();
        objectType.interfaces = Stream.of(objectTypes).flatMap(item -> Stream.ofNullable(item.getInterfaces()).flatMap(Collection::stream).distinct()).collect(Collectors.toCollection(LinkedHashSet::new));
        objectType.directives = Stream.of(objectTypes).flatMap(item -> io.vavr.collection.Stream.ofAll(Stream.ofNullable(item.getDirectives()).flatMap(Collection::stream)).distinctBy(Directive::getName).toJavaStream()).collect(Collectors.toCollection(LinkedHashSet::new));
        objectType.fields = objectTypes[0].getFields();
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

    public Collection<String> getInterfaces() {
        return interfaces;
    }

    public ObjectType setInterfaces(Collection<String> interfaces) {
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

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public ObjectType setDirectives(Collection<Directive> directives) {
        if (directives != null) {
            this.directives = new LinkedHashSet<>(directives);
        }
        return this;
    }

    public ObjectType addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive);
        return this;
    }

    public ObjectType addDirectives(Collection<Directive> directives) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.addAll(directives);
        return this;
    }

    public Collection<Field> getFields() {
        return fields;
    }

    public ObjectType setFields(Collection<Field> fields) {
        this.fields = fields;
        return this;
    }

    public ObjectType addFields(Collection<Field> fields) {
        if (this.fields == null) {
            this.fields = new LinkedHashSet<>();
        }
        this.fields.addAll(fields.stream().filter(field -> this.fields.stream().noneMatch(item -> item.getName().equals(field.getName()))).collect(Collectors.toCollection(LinkedHashSet::new)));
        return this;
    }

    public ObjectType addField(Field field) {
        if (this.fields == null) {
            this.fields = new LinkedHashSet<>();
        }
        if (this.fields.stream().noneMatch(item -> item.getName().equals(field.getName()))) {
            this.fields.add(field);
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/ObjectType.stg");
        ST st = stGroupFile.getInstanceOf("objectTypeDefinition");
        st.add("objectType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
