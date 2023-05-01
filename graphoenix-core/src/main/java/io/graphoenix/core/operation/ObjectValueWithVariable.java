package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import io.vavr.CheckedFunction2;
import jakarta.json.*;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationMirror;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectValueWithVariable extends AbstractMap<String, JsonValue> implements ValueWithVariable, JsonObject, Iterable<JsonValue> {

    private final Map<String, ValueWithVariable> objectValueWithVariable;

    public ObjectValueWithVariable() {
        this.objectValueWithVariable = new LinkedHashMap<>();
    }

    public ObjectValueWithVariable(Map<?, ?> objectValueWithVariable) {
        this.objectValueWithVariable = objectValueWithVariable.entrySet().stream().collect(Collectors.toMap(entry -> (String) entry.getKey(), entry -> ValueWithVariable.of(entry.getValue())));
    }

    public ObjectValueWithVariable(JsonObject objectValueWithVariable) {
        this.objectValueWithVariable = objectValueWithVariable.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> ValueWithVariable.of(entry.getValue())));
    }

    public ObjectValueWithVariable(AnnotationMirror objectValueWithVariable) {
        this.objectValueWithVariable = objectValueWithVariable.getElementValues().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getSimpleName().toString(), entry -> ValueWithVariable.of(entry.getValue())));
    }

    public ObjectValueWithVariable(Object objectValueWithVariable) {
        Class<?> clazz = objectValueWithVariable.getClass();
        CheckedFunction2<Field, Object, Object> getField = (field, object) -> {
            field.setAccessible(true);
            return field.get(object);
        };
        this.objectValueWithVariable = Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toMap(Field::getName, field -> ValueWithVariable.of(getField.unchecked().apply(field, objectValueWithVariable))));
    }

    public ObjectValueWithVariable(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        this.objectValueWithVariable = objectValueWithVariableContext.objectFieldWithVariable().stream().collect(Collectors.toMap(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText(), objectFieldWithVariableContext -> ValueWithVariable.of(objectFieldWithVariableContext.valueWithVariable())));
    }

    public ValueWithVariable put(String key, Object value) {
        return objectValueWithVariable.put(key, ValueWithVariable.of(value));
    }

    public ValueWithVariable putIfAbsent(String key, Object value) {
        return objectValueWithVariable.putIfAbsent(key, ValueWithVariable.of(value));
    }

    public ValueWithVariable compute(String key, Object value) {
        return objectValueWithVariable.compute(key, (k, v) -> ValueWithVariable.of(value));
    }

    public ValueWithVariable computeIfAbsent(String key, Object value) {
        return objectValueWithVariable.computeIfAbsent(key, k -> ValueWithVariable.of(value));
    }

    @NotNull
    @Override
    public Set<Entry<String, JsonValue>> entrySet() {
        return objectValueWithVariable.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (JsonValue) entry.getValue()))
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
        return objectValueWithVariable.size();
    }

    @Override
    public JsonValue get(Object key) {
        return objectValueWithVariable.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return objectValueWithVariable.containsKey(key);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Iterator<JsonValue> iterator() {
        return null;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/ObjectValueWithVariable.stg");
        ST st = stGroupFile.getInstanceOf("objectValueWithVariableDefinition");
        st.add("objectValueWithVariable", objectValueWithVariable);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
