package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.*;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayValueWithVariable extends AbstractList<JsonValue> implements ValueWithVariable, JsonArray {

    private final List<ValueWithVariable> valueWithVariables;

    public ArrayValueWithVariable(Arrays valueWithVariables) {
        this.valueWithVariables = Stream.of(valueWithVariables).map(ValueWithVariable::of).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        this.valueWithVariables = arrayValueWithVariableContext.valueWithVariable().stream().map(ValueWithVariable::of).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(Collection<?> valueWithVariables) {
        this.valueWithVariables = valueWithVariables.stream().map(ValueWithVariable::of).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(JsonArray valueWithVariables) {
        this.valueWithVariables = valueWithVariables.stream().map(ValueWithVariable::of).collect(Collectors.toList());
    }

    @Override
    public JsonObject getJsonObject(int index) {
        return (JsonObject) valueWithVariables.get(index);
    }

    @Override
    public JsonArray getJsonArray(int index) {
        return (JsonArray) valueWithVariables.get(index);
    }

    @Override
    public JsonNumber getJsonNumber(int index) {
        return (JsonNumber) valueWithVariables.get(index);
    }

    @Override
    public JsonString getJsonString(int index) {
        return (JsonString) valueWithVariables.get(index);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends JsonValue> List<T> getValuesAs(Class<T> clazz) {
        return (List<T>) valueWithVariables;
    }

    @Override
    public String getString(int index) {
        return getJsonString(index).getString();
    }

    @Override
    public String getString(int index, String defaultValue) {
        try {
            return getString(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public int getInt(int index) {
        return getJsonNumber(index).intValue();
    }

    @Override
    public int getInt(int index, int defaultValue) {
        try {
            return getInt(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(int index) {
        JsonValue jsonValue = get(index);
        if (jsonValue == JsonValue.TRUE) {
            return true;
        } else if (jsonValue == JsonValue.FALSE) {
            return false;
        } else {
            throw new ClassCastException();
        }
    }

    @Override
    public boolean getBoolean(int index, boolean defaultValue) {
        try {
            return getBoolean(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean isNull(int index) {
        return valueWithVariables.get(index).equals(JsonValue.NULL);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ARRAY;
    }

    @Override
    public JsonValue get(int index) {
        return valueWithVariables.get(index);
    }

    @Override
    public int size() {
        return valueWithVariables.size();
    }

    @Override
    public String toString() {
        return render();
    }

    @Override
    public String render() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/ArrayValueWithVariable.stg");
        ST st = stGroupFile.getInstanceOf("arrayValueWithVariableDefinition");
        st.add("valueWithVariables", valueWithVariables.stream().map(ValueWithVariable::render).collect(Collectors.toList()));
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
