package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class InterfaceType {

    private String name;
    private Set<String> interfaces;
    private Set<String> directives;
    private Set<Field> fields;
    private String description;

    public InterfaceType() {
    }

    public InterfaceType(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        this.name = interfaceTypeDefinitionContext.name().getText();
        if (interfaceTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(interfaceTypeDefinitionContext.description().StringValue());
        }
        if (interfaceTypeDefinitionContext.implementsInterfaces() != null) {
            this.interfaces = interfaceTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (interfaceTypeDefinitionContext.directives() != null) {
            this.directives = interfaceTypeDefinitionContext.directives().directive().stream().map(Directive::new).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (interfaceTypeDefinitionContext.fieldsDefinition() != null) {
            this.fields = interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(Field::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public static InterfaceType merge(GraphqlParser.InterfaceTypeDefinitionContext... interfaceTypeDefinitionContexts) {
        return merge(Stream.of(interfaceTypeDefinitionContexts).map(InterfaceType::new).toArray(InterfaceType[]::new));
    }

    public static InterfaceType merge(InterfaceType... interfaceTypes) {
        InterfaceType interfaceType = new InterfaceType();
        interfaceType.name = interfaceTypes[0].getName();
        interfaceType.description = interfaceTypes[0].getDescription();
        interfaceType.interfaces = Stream.of(interfaceTypes).flatMap(item -> Stream.ofNullable(item.getInterfaces()).flatMap(Collection::stream).distinct()).collect(Collectors.toCollection(LinkedHashSet::new));
        interfaceType.directives = Stream.of(interfaceTypes).flatMap(item -> Stream.ofNullable(item.getDirectives()).flatMap(Collection::stream).distinct()).collect(Collectors.toCollection(LinkedHashSet::new));
        interfaceType.fields = interfaceTypes[0].getFields();
        for (InterfaceType item : interfaceTypes) {
            for (Field itemField : item.getFields()) {
                if (interfaceType.fields.stream().noneMatch(field -> field.getName().equals(itemField.getName()))) {
                    interfaceType.fields.add(itemField);
                }
            }
        }
        return interfaceType;
    }

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

    public InterfaceType setDirectives(Set<Directive> directives) {
        if (directives != null) {
            this.directives = directives.stream().map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this;
    }

    public InterfaceType addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive.toString());
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
            this.fields = new LinkedHashSet<>();
        }
        this.fields.addAll(fields);
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
