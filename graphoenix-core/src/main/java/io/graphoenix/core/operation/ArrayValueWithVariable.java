package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonArray;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayValueWithVariable implements Collection<ValueWithVariable> {

    private final Collection<ValueWithVariable> valueWithVariables;

    public ArrayValueWithVariable(Arrays valueWithVariables) {
        this.valueWithVariables = Stream.of(valueWithVariables).map(ValueWithVariable::new).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        this.valueWithVariables = arrayValueWithVariableContext.valueWithVariable().stream().map(ValueWithVariable::new).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(Collection<?> valueWithVariables) {
        this.valueWithVariables = valueWithVariables.stream().map(ValueWithVariable::new).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(JsonArray valueWithVariables) {
        this.valueWithVariables = valueWithVariables.stream().map(ValueWithVariable::new).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/ArrayValueWithVariable.stg");
        ST st = stGroupFile.getInstanceOf("arrayValueWithVariableDefinition");
        st.add("valueWithVariables", valueWithVariables.stream().map(ValueWithVariable::toString).collect(Collectors.toList()));
        String render = st.render();
        stGroupFile.unload();
        return render;
    }

    @Override
    public int size() {
        return valueWithVariables.size();
    }

    @Override
    public boolean isEmpty() {
        return valueWithVariables.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return valueWithVariables.contains(o);
    }

    @NotNull
    @Override
    public Iterator<ValueWithVariable> iterator() {
        return valueWithVariables.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return valueWithVariables.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return valueWithVariables.toArray(a);
    }

    @Override
    public boolean add(ValueWithVariable valueWithVariable) {
        return valueWithVariables.add(valueWithVariable);
    }

    @Override
    public boolean remove(Object o) {
        return valueWithVariables.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return valueWithVariables.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends ValueWithVariable> c) {
        return valueWithVariables.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return valueWithVariables.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return valueWithVariables.retainAll(c);
    }

    @Override
    public void clear() {
        valueWithVariables.clear();
    }
}
