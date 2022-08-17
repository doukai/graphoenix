package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import io.vavr.CheckedFunction2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationMirror;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ObjectValueWithVariable implements Map<String, ValueWithVariable> {

    private final Map<String, ValueWithVariable> objectValueWithVariable;

    public ObjectValueWithVariable(Map<?, ?> objectValueWithVariable) {
        this.objectValueWithVariable = objectValueWithVariable.entrySet().stream().collect(Collectors.toMap(entry -> (String) entry.getKey(), entry -> new ValueWithVariable(entry.getValue())));
    }

    public ObjectValueWithVariable(AnnotationMirror objectValueWithVariable) {
        this.objectValueWithVariable = objectValueWithVariable.getElementValues().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getSimpleName().toString(), entry -> new ValueWithVariable(entry.getValue())));
    }

    public ObjectValueWithVariable(Object objectValueWithVariable) {
        Class<?> clazz = objectValueWithVariable.getClass();
        CheckedFunction2<Field, Object, Object> getField = (field, object) -> {
            field.setAccessible(true);
            return field.get(object);
        };
        this.objectValueWithVariable = Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toMap(Field::getName, field -> new ValueWithVariable(getField.unchecked().apply(field, objectValueWithVariable))));
    }

    public ObjectValueWithVariable(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        this.objectValueWithVariable = objectValueWithVariableContext.objectFieldWithVariable().stream().collect(Collectors.toMap(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText(), objectFieldWithVariableContext -> new ValueWithVariable(objectFieldWithVariableContext.valueWithVariable())));
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/ObjectValueWithVariable.stg");
        ST st = stGroupFile.getInstanceOf("objectValueWithVariableDefinition");
        st.add("objectValueWithVariable", objectValueWithVariable.keySet().stream().collect(Collectors.toMap(key -> key, key -> objectValueWithVariable.get(key).toString())));
        String render = st.render();
        stGroupFile.unload();
        return render;
    }

    @Override
    public int size() {
        return objectValueWithVariable.size();
    }

    @Override
    public boolean isEmpty() {
        return objectValueWithVariable.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return objectValueWithVariable.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return objectValueWithVariable.containsValue(value);
    }

    @Override
    public ValueWithVariable get(Object key) {
        return objectValueWithVariable.get(key);
    }

    @Nullable
    @Override
    public ValueWithVariable put(String key, ValueWithVariable value) {
        return objectValueWithVariable.put(key, value);
    }

    @Override
    public ValueWithVariable remove(Object key) {
        return objectValueWithVariable.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends ValueWithVariable> m) {
        objectValueWithVariable.putAll(m);
    }

    @Override
    public void clear() {
        objectValueWithVariable.clear();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return objectValueWithVariable.keySet();
    }

    @NotNull
    @Override
    public Collection<ValueWithVariable> values() {
        return objectValueWithVariable.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, ValueWithVariable>> entrySet() {
        return objectValueWithVariable.entrySet();
    }
}
