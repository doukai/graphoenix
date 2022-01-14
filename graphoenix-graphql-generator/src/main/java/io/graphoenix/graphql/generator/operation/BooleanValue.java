package io.graphoenix.graphql.generator.operation;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class BooleanValue {

    private Boolean value;

    public BooleanValue(AnnotationValue value) {
        this.value = (Boolean) value.getValue();
    }

    public BooleanValue(Boolean value) {
        this.value = value;
    }

    public BooleanValue(TerminalNode value) {
        this.value = Boolean.valueOf(value.getText());
    }

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/operation/BooleanValue.stg").getInstanceOf("booleanValueDefinition");
        st.add("booleanValue", this);
        return st.render();
    }
}
