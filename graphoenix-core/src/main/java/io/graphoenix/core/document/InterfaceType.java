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

public class InterfaceType {

    private String name;
    private Collection<String> interfaces;
    private Collection<Directive> directives;
    private Collection<Field> fields;
    private String description;

    public InterfaceType() {
    }

    public InterfaceType(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        this.name = interfaceTypeDefinitionContext.name().getText();
        if (interfaceTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(interfaceTypeDefinitionContext.description().StringValue());
        }
        if (interfaceTypeDefinitionContext.implementsInterfaces() != null) {
            this.interfaces = getInterfaces(interfaceTypeDefinitionContext.implementsInterfaces()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (interfaceTypeDefinitionContext.directives() != null) {
            this.directives = interfaceTypeDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (interfaceTypeDefinitionContext.fieldsDefinition() != null) {
            this.fields = interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(Field::new).collect(Collectors.toCollection(LinkedHashSet::new));
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

    public static InterfaceType merge(GraphqlParser.InterfaceTypeDefinitionContext... interfaceTypeDefinitionContexts) {
        return merge(Stream.of(interfaceTypeDefinitionContexts).map(InterfaceType::new).toArray(InterfaceType[]::new));
    }

    public static InterfaceType merge(InterfaceType... interfaceTypes) {
        InterfaceType interfaceType = new InterfaceType();
        interfaceType.name = interfaceTypes[0].getName();
        interfaceType.description = interfaceTypes[0].getDescription();
        interfaceType.interfaces = Stream.of(interfaceTypes).flatMap(item -> Stream.ofNullable(item.getInterfaces()).flatMap(Collection::stream).distinct()).collect(Collectors.toCollection(LinkedHashSet::new));
        interfaceType.directives = Stream.of(interfaceTypes).flatMap(item -> io.vavr.collection.Stream.ofAll(Stream.ofNullable(item.getDirectives()).flatMap(Collection::stream)).distinctBy(Directive::getName).toJavaStream()).collect(Collectors.toCollection(LinkedHashSet::new));
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

    public Collection<String> getInterfaces() {
        return interfaces;
    }

    public InterfaceType setInterfaces(Collection<String> interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public InterfaceType setDirectives(Collection<Directive> directives) {
        if (directives != null) {
            this.directives = new LinkedHashSet<>(directives);
        }
        return this;
    }

    public InterfaceType addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive);
        return this;
    }

    public Collection<Field> getFields() {
        return fields;
    }

    public InterfaceType setFields(Collection<Field> fields) {
        this.fields = fields;
        return this;
    }

    public InterfaceType addFields(Collection<Field> fields) {
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
