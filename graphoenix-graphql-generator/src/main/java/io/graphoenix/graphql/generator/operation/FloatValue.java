package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class FloatValue {

    private Number value;

    public FloatValue(AnnotationValue value) {
        this.value = (Number) value.getValue();
    }

    public FloatValue(Number value) {
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
        ST st = new STGroupFile("stg/operation/FloatValue.stg").getInstanceOf("floatValueDefinition");
        st.add("floatValue", this);
        return st.render();
    }
}
