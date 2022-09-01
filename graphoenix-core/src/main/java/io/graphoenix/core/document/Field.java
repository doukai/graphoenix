package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class Field {

    private String name;
    private Set<InputValue> arguments;
    private String typeName;
    private Set<String> directives;
    private String description;

    public Field() {
    }

    public Field(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        this.name = fieldDefinitionContext.name().getText();
        this.typeName = fieldDefinitionContext.type().getText();
        if (fieldDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(fieldDefinitionContext.description().StringValue());
        }
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            this.arguments = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(InputValue::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (fieldDefinitionContext.directives() != null) {
            this.directives = fieldDefinitionContext.directives().directive().stream().map(Directive::new).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public Set<InputValue> getArguments() {
        return arguments;
    }

    public Field setArguments(Set<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field addArguments(Set<InputValue> arguments) {
        if (this.arguments == null) {
            this.arguments = new LinkedHashSet<>();
        }
        this.arguments.addAll(arguments);
        return this;
    }

    public Field addArgument(InputValue argument) {
        if (this.arguments == null) {
            this.arguments = new LinkedHashSet<>();
        }
        this.arguments.add(argument);
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public Field setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public Field setStringDirectives(Set<String> directives) {
        if (directives != null) {
            this.directives = directives.stream().map(directive -> !directive.startsWith("@") ? "@".concat(directive) : directive).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this;
    }

    public Field setDirectives(Set<Directive> directives) {
        if (directives != null) {
            this.directives = directives.stream().map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this;
    }

    public Field addStringDirective(String directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        if (!directive.startsWith("@")) {
            directive = "@".concat(directive);
        }
        this.directives.add(directive);
        return this;
    }

    public Field addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive.toString());
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Field setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/Field.stg");
        ST st = stGroupFile.getInstanceOf("fieldDefinition");
        st.add("filed", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
