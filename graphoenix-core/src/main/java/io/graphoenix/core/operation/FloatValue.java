package io.graphoenix.core.operation;

import org.antlr.v4.runtime.tree.TerminalNode;
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

    public FloatValue(TerminalNode value) {
        this.value = Float.valueOf(value.getText());
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/FloatValue.stg");
        ST st = stGroupFile.getInstanceOf("floatValueDefinition");
        st.add("floatValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
