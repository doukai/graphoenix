package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.document.Directive;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Field {

    private String name;
    private String alias;
    private Arguments arguments;
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
            this.arguments = new Arguments(fieldContext.arguments());
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

    public Arguments getArguments() {
        return arguments;
    }

    public Field setArguments(Arguments arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field setArguments(JsonObject jsonObject) {
        this.arguments = new Arguments(jsonObject);
        return this;
    }

    public Field addArguments(Arguments arguments) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.putAll(arguments);
        return this;
    }

    public Field addArguments(JsonObject jsonObject) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.putAll(jsonObject);
        return this;
    }

    public Field addArgument(String name, Object valueWithVariable) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.put(name, valueWithVariable);
        return this;
    }

    public Field addArgument(String name, ValueWithVariable valueWithVariable) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.put(name, valueWithVariable);
        return this;
    }

    public Field addArgument(String name, JsonValue valueWithVariable) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.put(name, valueWithVariable);
        return this;
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

    public Field addFields(Collection<Field> fields) {
        if (this.fields == null) {
            this.fields = new LinkedHashSet<>();
        }
        this.fields.addAll(fields);
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
