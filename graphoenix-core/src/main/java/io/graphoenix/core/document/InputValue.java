package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class InputValue {

    private String name;
    private String typeName;
    private String defaultValue;
    private Set<String> directives;
    private String description;

    public InputValue() {
    }

    public InputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        this.name = inputValueDefinitionContext.name().getText();
        this.typeName = inputValueDefinitionContext.type().getText();
        if (inputValueDefinitionContext.defaultValue() != null) {
            this.defaultValue = inputValueDefinitionContext.defaultValue().value().getText();
        }
        if (inputValueDefinitionContext.directives() != null) {
            this.directives = inputValueDefinitionContext.directives().directive().stream().map(Directive::new).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (inputValueDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(inputValueDefinitionContext.description().StringValue());
        }
    }

    public InputValue(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public InputValue setName(String name) {
        this.name = name;
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public InputValue setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public InputValue setDefaultValue(String defaultValue) {
        if (defaultValue != null) {
            if (this.getTypeName() != null && (this.getTypeName().equals("String") || this.getTypeName().equals("String!"))) {
                this.defaultValue = "\"".concat(defaultValue).concat("\"");
            } else {
                this.defaultValue = defaultValue;
            }
        }
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public InputValue setDirectives(Set<Directive> directives) {
        if (directives != null) {
            this.directives = directives.stream().map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this;
    }

    public String getDescription() {
        return description;
    }

    public InputValue setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/InputValue.stg");
        ST st = stGroupFile.getInstanceOf("inputValueDefinition");
        st.add("inputValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
