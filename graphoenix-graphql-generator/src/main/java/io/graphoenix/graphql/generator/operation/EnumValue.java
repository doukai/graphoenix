package io.graphoenix.graphql.generator.operation;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class EnumValue {

    private String value;

    public EnumValue(AnnotationValue value) {
        this.value = value.getValue().toString();
    }

    public EnumValue(Enum<?> value) {
        this.value = value.name();
    }

    public EnumValue(GraphqlParser.EnumValueContext enumValueContext) {
        this.value = enumValueContext.getText();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/operation/EnumValue.stg").getInstanceOf("enumValueDefinition");
        st.add("enumValue", this);
        return st.render();
    }
}
