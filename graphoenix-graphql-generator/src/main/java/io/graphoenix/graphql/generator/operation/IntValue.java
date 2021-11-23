package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class IntValue {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/IntValue.stg");

    private Number value;

    public IntValue(AnnotationValue value) {
        this.value = (Number) value.getValue();
    }

    public IntValue(Number value) {
        this.value = value;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("intValueDefinition");
        st.add("intValue", this);
        return st.render();
    }
}
