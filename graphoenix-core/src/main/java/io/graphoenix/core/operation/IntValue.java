package io.graphoenix.core.operation;

import jakarta.json.JsonNumber;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationValue;
import java.math.BigDecimal;
import java.math.BigInteger;

public class IntValue implements ValueWithVariable, JsonNumber {

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
    public boolean isIntegral() {
        return true;
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public int intValueExact() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    @Override
    public long longValueExact() {
        return value.longValue();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return BigInteger.valueOf(value.longValue());
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return BigInteger.valueOf(value.longValue());
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(value.doubleValue());
    }

    @Override
    public ValueType getValueType() {
        return ValueType.NUMBER;
    }

    @Override
    public String toString() {
        return render();
    }

    @Override
    public String render() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/IntValue.stg");
        ST st = stGroupFile.getInstanceOf("intValueDefinition");
        st.add("intValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
