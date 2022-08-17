package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.RuleContext;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

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

    public Field() {
    }

    public Field(String name) {
        this.name = name;
    }

    public Field(GraphqlParser.SelectionContext selectionContext) {
        this.name = selectionContext.field().name().getText();
        if (selectionContext.field().alias() != null) {
            this.alias = selectionContext.field().alias().name().getText();
        }
        if (selectionContext.field().arguments() != null) {
            this.arguments = selectionContext.field().arguments().argument().stream()
                    .map(Argument::new)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (selectionContext.field().selectionSet() != null) {
            this.fields = selectionContext.field().selectionSet().selection().stream()
                    .map(Field::new)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (selectionContext.field().directives() != null) {
            this.directives = selectionContext.field().directives().directive().stream()
                    .map(RuleContext::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
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

    public Optional<Argument> getArgument(String name) {
        return arguments.stream().filter(argument -> argument.getName().equals(name)).findFirst();
    }

    public Optional<ValueWithVariable> getValueWithVariable(String name) {
        return getArgument(name).map(Argument::getValueWithVariable);
    }

    public Field addArgument(Argument argument) {
        if (this.arguments == null) {
            this.arguments = new LinkedHashSet<>();
        }
        this.arguments.add(argument);
        return this;
    }

    public Field addArguments(Stream<Argument> argumentStream) {
        if (this.arguments == null) {
            this.arguments = new LinkedHashSet<>();
        }
        this.arguments.addAll(argumentStream.collect(Collectors.toList()));
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
