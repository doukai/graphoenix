package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.document.Variable;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Operation {

    private String operationType;
    private String name;
    private Collection<Variable> variables;
    private Collection<Directive> directives;
    private Collection<Field> fields;

    public Operation() {
    }

    public Operation(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() != null) {
            this.operationType = operationDefinitionContext.operationType().getText();
        } else {
            this.operationType = "query";
        }
        if (operationDefinitionContext.name() != null) {
            this.name = operationDefinitionContext.name().getText();
        }
        if (operationDefinitionContext.variableDefinitions() != null) {
            this.variables = operationDefinitionContext.variableDefinitions().variableDefinition().stream().map(Variable::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (operationDefinitionContext.directives() != null) {
            this.directives = operationDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        this.fields = operationDefinitionContext.selectionSet().selection().stream().map(Field::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public String getOperationType() {
        return operationType;
    }

    public Operation setOperationType(String operationType) {
        this.operationType = operationType;
        return this;
    }

    public String getName() {
        return name;
    }

    public Operation setName(String name) {
        this.name = name;
        return this;
    }

    public Collection<Variable> getVariableDefinitions() {
        return variables;
    }

    public Operation setVariableDefinitions(Collection<Variable> variables) {
        this.variables = variables;
        return this;
    }

    public Operation addVariableDefinition(Variable variable) {
        if (this.variables == null) {
            this.variables = new LinkedHashSet<>();
        }
        this.variables.add(variable);
        return this;
    }

    public Operation addVariableDefinitions(Stream<Variable> variableDefinitionStream) {
        if (this.variables == null) {
            this.variables = new LinkedHashSet<>();
        }
        this.variables.addAll(variableDefinitionStream.collect(Collectors.toList()));
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public Operation setDirectives(Collection<Directive> directives) {
        this.directives = directives;
        return this;
    }

    public Operation addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive);
        return this;
    }

    public Collection<Field> getFields() {
        return fields;
    }

    public Operation setFields(Collection<Field> fields) {
        this.fields = fields;
        return this;
    }

    public Operation addFields(Collection<Field> fields) {
        if (this.fields == null) {
            this.fields = new LinkedHashSet<>();
        }
        this.fields.addAll(fields);
        return this;
    }

    public Field getField(String name) {
        return this.fields.stream().filter(field -> field.getAlias() != null && field.getAlias().equals(name) || field.getName().equals(name)).findFirst().orElse(null);
    }

    public Operation addField(Field field) {
        if (this.fields == null) {
            this.fields = new LinkedHashSet<>();
        }
        this.fields.add(field);
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Operation.stg");
        ST st = stGroupFile.getInstanceOf("operationDefinition");
        st.add("operation", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
