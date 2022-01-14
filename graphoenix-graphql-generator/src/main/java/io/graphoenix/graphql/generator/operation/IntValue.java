package io.graphoenix.graphql.generator.operation;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class IntValue {

    private Number value;

    public IntValue(AnnotationValue value) {
        this.value = (Number) value.getValue();
    }

    public IntValue(Number value) {
        this.value = value;
    }

    public IntValue(TerminalNode value) {
        this.value = Integer.valueOf(value.getText());
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/operation/IntValue.stg").getInstanceOf("intValueDefinition");
        st.add("intValue", this);
        return st.render();
    }
}
