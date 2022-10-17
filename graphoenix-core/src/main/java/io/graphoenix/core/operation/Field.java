package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.document.Directive;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonCollectors;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Field {

    private String name;
    private String alias;
    private Set<Argument> arguments;
    private Set<String> directives;
    private Set<Field> fields;
    private String selections;

    public Field() {
    }

    public Field(String name) {
        this.name = name;
    }

    public Field(GraphqlParser.SelectionContext selectionContext) {
        this(selectionContext.field());
    }

    public Field(GraphqlParser.FieldContext fieldContext) {
        this.name = fieldContext.name().getText();
        if (fieldContext.alias() != null) {
            this.alias = fieldContext.alias().name().getText();
        }
        if (fieldContext.arguments() != null) {
            this.arguments = fieldContext.arguments().argument().stream().map(Argument::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (fieldContext.selectionSet() != null) {
            this.fields = fieldContext.selectionSet().selection().stream().map(Field::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (fieldContext.directives() != null) {
            this.directives = fieldContext.directives().directive().stream().map(directiveContext -> new Directive(directiveContext).toString()).collect(Collectors.toCollection(LinkedHashSet::new));

        }
    }

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public Field setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public Set<Argument> getArguments() {
        return arguments;
    }

    public Field setArguments(Set<Argument> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field setArguments(JsonObject jsonObject) {
        this.arguments = jsonObject.entrySet().stream().map(entry -> new Argument().setName(entry.getKey()).setValueWithVariable(entry.getValue())).collect(Collectors.toSet());
        return this;
    }

    public Optional<Argument> getArgument(String name) {
        return arguments.stream().filter(argument -> argument.getName().equals(name)).findFirst();
    }

    public Argument getOrCreateArgument(String name) {
        Optional<Argument> argument = getArgument(name);
        if (argument.isPresent()) {
            return argument.get();
        } else {
            Argument newArgument = new Argument(name);
            this.arguments.add(newArgument);
            return newArgument;
        }
    }

    public Optional<ValueWithVariable> getValueWithVariable(String name) {
        return getArgument(name).map(Argument::getValueWithVariable);
    }

    public ValueWithVariable getValueWithVariableOrEmpty(String name) {
        return getArgument(name).map(Argument::getValueWithVariable).orElseGet(() -> new ValueWithVariable(new NullValue()));
    }

    public Field addArgument(Argument argument) {
        if (this.arguments == null) {
            this.arguments = new LinkedHashSet<>();
        }
        this.arguments.add(argument);
        return this;
    }

    public Field addArgument(String name, JsonValue jsonValue) {
        this.addArgument(new Argument(name, jsonValue));
        return this;
    }

    public Field addArguments(Stream<Argument> argumentStream) {
        if (this.arguments == null) {
            this.arguments = new LinkedHashSet<>();
        }
        this.arguments.addAll(argumentStream.collect(Collectors.toList()));
        return this;
    }

    public JsonObject getArgumentsJson() {
        return this.getArguments().stream()
                .map(argument -> new AbstractMap.SimpleEntry<>(argument.getName(), argument.getValueWithVariable().toJson()))
                .collect(JsonCollectors.toJsonObject());
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public Field setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public Field setFields(Set<Field> fields) {
        this.fields = fields;
        return this;
    }

    public Field addField(Field field) {
        if (this.fields == null) {
            this.fields = new LinkedHashSet<>();
        }
        this.fields.add(field);
        return this;
    }

    public String getSelections() {
        return selections;
    }

    public Field setSelections(String selections) {
        this.selections = selections;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Field.stg");
        ST st = stGroupFile.getInstanceOf("fieldDefinition");
        st.add("filed", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
