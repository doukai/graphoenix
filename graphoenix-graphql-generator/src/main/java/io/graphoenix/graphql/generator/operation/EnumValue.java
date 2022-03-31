package io.graphoenix.graphql.generator.operation;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class EnumValue {

    private String value;

    public EnumValue(String value) {
        this.value = value;
    }

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
        STGroupFile stGroupFile = new STGroupFile("stg/operation/EnumValue.stg");
        ST st = stGroupFile.getInstanceOf("enumValueDefinition");
        st.add("enumValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
