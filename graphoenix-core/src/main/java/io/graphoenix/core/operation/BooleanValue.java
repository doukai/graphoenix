package io.graphoenix.core.operation;

import jakarta.json.JsonValue;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;

public class BooleanValue implements ValueWithVariable, JsonValue {

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
    public ValueType getValueType() {
        return value ? ValueType.TRUE : ValueType.FALSE;
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/BooleanValue.stg");
        ST st = stGroupFile.getInstanceOf("booleanValueDefinition");
        st.add("booleanValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
