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

public class EnumType {

    private String name;
    private Collection<Directive> directives;
    private Collection<EnumValue> enumValues;
    private String description;

    public EnumType() {
    }

    public EnumType(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        this.name = enumTypeDefinitionContext.name().getText();
        if (enumTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(enumTypeDefinitionContext.description().StringValue());
        }
        if (enumTypeDefinitionContext.directives() != null) {
            this.directives = enumTypeDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (enumTypeDefinitionContext.enumValueDefinitions() != null) {
            this.enumValues = enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream().map(EnumValue::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public static EnumType merge(GraphqlParser.EnumTypeDefinitionContext... enumTypeDefinitionContexts) {
        return merge(Stream.of(enumTypeDefinitionContexts).map(EnumType::new).toArray(EnumType[]::new));
    }

    public static EnumType merge(EnumType... enumTypes) {
        EnumType enumType = new EnumType();
        enumType.name = enumTypes[0].getName();
        enumType.description = enumTypes[0].getDescription();
        enumType.directives = io.vavr.collection.Stream.ofAll(Stream.of(enumTypes).flatMap(item -> Stream.ofNullable(item.getDirectives()).flatMap(Collection::stream))).distinctBy(Directive::getName).collect(Collectors.toCollection(LinkedHashSet::new));
        enumType.enumValues = enumTypes[0].getEnumValues();
        for (EnumType item : enumTypes) {
            for (EnumValue itemEnumValue : item.getEnumValues()) {
                if (enumType.enumValues.stream().noneMatch(enumValue -> enumValue.getName().equals(itemEnumValue.getName()))) {
                    enumType.enumValues.add(itemEnumValue);
                }
            }
        }
        return enumType;
    }

    public String getName() {
        return name;
    }

    public EnumType setName(String name) {
        this.name = name;
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public EnumType setDirectives(Collection<Directive> directives) {
        if (directives != null) {
            this.directives = new LinkedHashSet<>(directives);
        }
        return this;
    }

    public EnumType addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive);
        return this;
    }

    public EnumType addDirectives(Collection<Directive> directives) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.addAll(directives);
        return this;
    }

    public Collection<EnumValue> getEnumValues() {
        return enumValues;
    }

    public EnumType setEnumValues(Collection<EnumValue> enumValues) {
        this.enumValues = enumValues;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EnumType setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/EnumType.stg");
        ST st = stGroupFile.getInstanceOf("enumTypeDefinition");
        st.add("enumType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
