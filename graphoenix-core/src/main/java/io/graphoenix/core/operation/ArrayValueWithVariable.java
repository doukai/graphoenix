package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayValueWithVariable {

    private Collection<?> valueWithVariables;

    public ArrayValueWithVariable(Arrays valueWithVariables) {
        this.valueWithVariables = Stream.of(valueWithVariables).map(ValueWithVariable::new).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        this.valueWithVariables = arrayValueWithVariableContext.valueWithVariable().stream().map(ValueWithVariable::new).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(Collection<?> valueWithVariables) {
        this(valueWithVariables, true);
    }

    public ArrayValueWithVariable(Collection<?> valueWithVariables, boolean toValueWithVariable) {
        if (toValueWithVariable) {
            this.valueWithVariables = valueWithVariables.stream().map(ValueWithVariable::new).collect(Collectors.toList());
        } else {
            this.valueWithVariables = valueWithVariables;
        }
    }

    public Collection<?> getValueWithVariables() {
        return valueWithVariables;
    }

    public void setValueWithVariables(Collection<?> valueWithVariables) {
        this.valueWithVariables = valueWithVariables;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/ArrayValueWithVariable.stg");
        ST st = stGroupFile.getInstanceOf("arrayValueWithVariableDefinition");
        st.add("valueWithVariables", valueWithVariables.stream().map(Object::toString).collect(Collectors.toList()));
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
