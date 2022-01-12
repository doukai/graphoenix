package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class StringValue {

    private String value;

    public StringValue(AnnotationValue value) {
        this.value = (String) value.getValue();
    }

    public StringValue(Character value) {
        this.value = value.toString();
    }

    public StringValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/operation/StringValue.stg").getInstanceOf("stringValueDefinition");
        st.add("stringValue", this);
        return st.render();
    }
}
