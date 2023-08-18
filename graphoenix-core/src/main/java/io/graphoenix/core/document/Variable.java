package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.operation.Directive;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class Variable {

    private io.graphoenix.core.operation.Variable variable;
    private String typeName;
    private String defaultValue;
    private Collection<Directive> directives;

    public Variable() {
    }

    public Variable(GraphqlParser.VariableDefinitionContext variableDefinitionContext) {
        if (variableDefinitionContext.variable() != null) {
            this.variable = new io.graphoenix.core.operation.Variable(variableDefinitionContext.variable());
        }
        if (variableDefinitionContext.type() != null) {
            this.typeName = variableDefinitionContext.type().getText();
        }
        if (variableDefinitionContext.defaultValue() != null) {
            this.defaultValue = variableDefinitionContext.defaultValue().value().getText();
        }
        if (variableDefinitionContext.directives() != null) {
            this.directives = variableDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public io.graphoenix.core.operation.Variable getVariable() {
        return variable;
    }

    public Variable setVariable(io.graphoenix.core.operation.Variable variable) {
        this.variable = variable;
        return this;
    }

    public Variable setVariable(String variableName) {
        this.variable = new io.graphoenix.core.operation.Variable(variableName);
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public Variable setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Variable setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public Variable setDirectives(Collection<Directive> directives) {
        this.directives = directives;
        return this;
    }

    public Variable addDirectives(Collection<Directive> directives) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.addAll(directives);
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/Variable.stg");
        ST st = stGroupFile.getInstanceOf("variableDefinition");
        st.add("variable", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
