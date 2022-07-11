package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.RuleContext;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class EnumType {

    private String name;
    private Set<String> directives;
    private Set<EnumValue> enumValues;
    private String description;

    public EnumType() {
    }

    public EnumType(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        this.name = enumTypeDefinitionContext.name().getText();
        if (enumTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(enumTypeDefinitionContext.description().StringValue());
        }
        if (enumTypeDefinitionContext.directives() != null) {
            this.directives = enumTypeDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toSet());
        }
        if (enumTypeDefinitionContext.enumValueDefinitions() != null) {
            this.enumValues = enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream().map(EnumValue::new).collect(Collectors.toSet());
        }
    }

    public static EnumType merge(GraphqlParser.EnumTypeDefinitionContext... enumTypeDefinitionContexts) {
        return merge(Stream.of(enumTypeDefinitionContexts).map(EnumType::new).toArray(EnumType[]::new));
    }

    public static EnumType merge(EnumType... enumTypes) {
        EnumType enumType = new EnumType();
        enumType.name = enumTypes[0].getName();
        enumType.description = enumTypes[0].getDescription();
        enumType.directives = Stream.of(enumTypes).flatMap(item -> Stream.ofNullable(item.getDirectives()).flatMap(Collection::stream).distinct()).collect(Collectors.toSet());
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

    public Set<String> getDirectives() {
        return directives;
    }

    public EnumType setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<EnumValue> getEnumValues() {
        return enumValues;
    }

    public EnumType setEnumValues(Set<EnumValue> enumValues) {
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
