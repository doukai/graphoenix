package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.operation.Directive;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class EnumValue {

    private String name;
    private Collection<Directive> directives;
    private String description;

    public EnumValue() {
    }

    public EnumValue(String name) {
        this.name = name;
    }

    public EnumValue(GraphqlParser.EnumValueDefinitionContext enumValueDefinitionContext) {
        this.name = enumValueDefinitionContext.enumValue().enumValueName().getText();
        if (enumValueDefinitionContext.directives() != null) {
            this.directives = enumValueDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (enumValueDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(enumValueDefinitionContext.description().StringValue());
        }
    }

    public String getName() {
        return name;
    }

    public EnumValue setName(String name) {
        this.name = name;
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public EnumValue setDirectives(Collection<Directive> directives) {
        if (directives != null) {
            this.directives = new LinkedHashSet<>(directives);
        }
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EnumValue setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/EnumValue.stg");
        ST st = stGroupFile.getInstanceOf("enumValueDefinition");
        st.add("enumValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
