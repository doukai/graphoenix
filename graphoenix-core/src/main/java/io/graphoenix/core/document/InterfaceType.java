package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.RuleContext;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

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
            this.interfaces = interfaceTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toSet());
        }
        if (interfaceTypeDefinitionContext.directives() != null) {
            this.directives = interfaceTypeDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toSet());
        }
        if (interfaceTypeDefinitionContext.fieldsDefinition() != null) {
            this.fields = interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(Field::new).collect(Collectors.toSet());
        }
    }

    public static InterfaceType merge(GraphqlParser.InterfaceTypeDefinitionContext... interfaceTypeDefinitionContexts) {
        return merge(Stream.of(interfaceTypeDefinitionContexts).map(InterfaceType::new).toArray(InterfaceType[]::new));
    }

    public static InterfaceType merge(InterfaceType... interfaceTypes) {
        InterfaceType interfaceType = new InterfaceType();
        interfaceType.name = interfaceTypes[0].getName();
        interfaceType.description = interfaceTypes[0].getDescription();
        interfaceType.interfaces = Stream.of(interfaceTypes).flatMap(item -> item.getInterfaces().stream()).collect(Collectors.toSet());
        interfaceType.directives = Stream.of(interfaceTypes).flatMap(item -> item.getDirectives().stream()).collect(Collectors.toSet());
        interfaceType.fields = Stream.of(interfaceTypes).flatMap(item -> item.getFields().stream()).collect(Collectors.toSet());
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
