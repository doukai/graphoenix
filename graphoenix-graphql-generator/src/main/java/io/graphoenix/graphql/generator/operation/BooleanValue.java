package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class BooleanValue {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/BooleanValue.stg");

    private Boolean value;

    public BooleanValue(AnnotationValue value) {
        this.value = (Boolean) value.getValue();
    }

    public BooleanValue(Boolean value) {
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("booleanValueDefinition");
        st.add("booleanValue", this);
        return st.render();
    }
}
