package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.operation.Directive;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class InputValue {

    private String name;
    private Type type;
    private String defaultValue;
    private Collection<Directive> directives;
    private String description;

    public InputValue() {
    }

    public InputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        this.name = inputValueDefinitionContext.name().getText();
        this.type = Type.of(inputValueDefinitionContext.type());
        if (inputValueDefinitionContext.defaultValue() != null) {
            this.defaultValue = inputValueDefinitionContext.defaultValue().value().getText();
        }
        if (inputValueDefinitionContext.directives() != null) {
            this.directives = inputValueDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
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

    public Type getType() {
        return type;
    }

    public InputValue setType(Type type) {
        this.type = type;
        return this;
    }

    public InputValue setType(String typeName) {
        this.type = Type.of(typeName);
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public InputValue setDefaultValue(String defaultValue) {
        if (defaultValue != null) {
            if (this.type.getTypeName().getName().equals("String")) {
                this.defaultValue = "\"" + defaultValue + "\"";
            } else {
                this.defaultValue = defaultValue;
            }
        }
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public InputValue setDirectives(Collection<Directive> directives) {
        if (directives != null) {
            this.directives = new LinkedHashSet<>(directives);
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
