package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.*;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.*;
import java.util.stream.Collectors;

public class Arguments extends AbstractMap<String, JsonValue> implements ValueWithVariable, JsonObject {

    private final Map<String, ValueWithVariable> arguments;

    public Arguments() {
        this.arguments = new LinkedHashMap<>();
    }

    public Arguments(GraphqlParser.ArgumentsContext argumentsContext) {
        this.arguments = argumentsContext.argument().stream()
                .map(argumentContext -> new SimpleEntry<>(argumentContext.name().getText(), ValueWithVariable.of(argumentContext.valueWithVariable())))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    public Arguments(JsonObject jsonObject) {
        this.arguments = jsonObject.entrySet().stream()
                .map(entry -> new SimpleEntry<>(entry.getKey(), ValueWithVariable.of(entry.getValue())))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    @Override
    public JsonValue put(String key, JsonValue value) {
        return put(key, (Object) value);
    }

    public ValueWithVariable put(String key, Object value) {
        return arguments.put(key, ValueWithVariable.of(value));
    }

    @NotNull
    @Override
    public Set<Entry<String, JsonValue>> entrySet() {
        return arguments.entrySet().stream()
                .map(entry -> new SimpleEntry<>(entry.getKey(), (JsonValue) entry.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    public JsonArray getJsonArray(String name) {
        return (JsonArray) get(name);
    }

    @Override
    public JsonObject getJsonObject(String name) {
        return (JsonObject) get(name);
    }

    @Override
    public JsonNumber getJsonNumber(String name) {
        return (JsonNumber) get(name);
    }

    @Override
    public JsonString getJsonString(String name) {
        return (JsonString) get(name);
    }

    @Override
    public String getString(String name) {
        return getJsonString(name).getString();
    }

    @Override
    public String getString(String name, String defaultValue) {
        JsonValue value = get(name);
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        } else {
            return defaultValue;
        }
    }

    @Override
    public int getInt(String name) {
        return getJsonNumber(name).intValue();
    }

    @Override
    public int getInt(String name, int defaultValue) {
        JsonValue value = get(name);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).intValue();
        } else {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String name) {
        JsonValue value = get(name);
        if (value == null) {
            throw new NullPointerException();
        } else if (value == JsonValue.TRUE) {
            return true;
        } else if (value == JsonValue.FALSE) {
            return false;
        } else {
            throw new ClassCastException();
        }
    }

    @Override
    public boolean getBoolean(String name, boolean defaultValue) {
        JsonValue value = get(name);
        if (value == JsonValue.TRUE) {
            return true;
        } else if (value == JsonValue.FALSE) {
            return false;
        } else {
            return defaultValue;
        }
    }

    @Override
    public boolean isNull(String name) {
        return get(name).equals(JsonValue.NULL);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.OBJECT;
    }

    @Override
    public JsonObject asJsonObject() {
        return this;
    }

    @Override
    public int size() {
        return arguments.size();
    }

    @Override
    public JsonValue get(Object key) {
        return arguments.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return arguments.containsKey(key);
    }

    @Override
    public String toString() {
        return render();
    }

    @Override
    public String render() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Arguments.stg");
        ST st = stGroupFile.getInstanceOf("argumentsDefinition");
        st.add("arguments", arguments.entrySet().stream().map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().render())).collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
