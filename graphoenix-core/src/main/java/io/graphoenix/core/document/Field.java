package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.operation.Directive;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.DATA_TYPE_DIRECTIVE_NAME;

public class Field {

    private String name;
    private Collection<InputValue> arguments;
    private Type type;
    private Collection<Directive> directives;
    private String description;

    public Field() {
    }

    public Field(String name) {
        this.name = name;
    }

    public Field(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        this.name = fieldDefinitionContext.name().getText();
        this.type = Type.of(fieldDefinitionContext.type());
        if (fieldDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(fieldDefinitionContext.description().StringValue());
        }
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            this.arguments = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(InputValue::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (fieldDefinitionContext.directives() != null) {
            this.directives = fieldDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public Collection<InputValue> getArguments() {
        return arguments;
    }

    public Field setArguments(Collection<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field addArguments(Collection<InputValue> arguments) {
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

    public Type getType() {
        return type;
    }

    public Field setType(Type type) {
        this.type = type;
        return this;
    }

    public Field setType(String typeName) {
        this.type = Type.of(typeName);
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public Field setStringDirectives(Collection<Directive> directives) {
        this.directives = directives;
        return this;
    }

    public Field setDirectives(Collection<Directive> directives) {
        if (directives != null) {
            this.directives = new LinkedHashSet<>(directives);
        }
        return this;
    }

    public Field addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive);
        return this;
    }

    public Field addDirectives(Collection<Directive> directives) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.addAll(directives);
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Field setDescription(String description) {
        this.description = description;
        return this;
    }

    public Optional<String> getDataTypeName() {
        return Stream.ofNullable(this.directives).flatMap(Collection::stream)
                .filter(directive -> directive.getName().equals(DATA_TYPE_DIRECTIVE_NAME))
                .flatMap(directive -> directive.getArguments().entrySet().stream())
                .filter(entry -> entry.getKey().equals("type"))
                .filter(entry -> entry.getValue().getValueType().equals(JsonValue.ValueType.STRING))
                .map(entry -> ((JsonString) entry.getValue()).getString())
                .findFirst();
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
