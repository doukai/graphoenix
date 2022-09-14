package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonArray;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayValueWithVariable implements List<ValueWithVariable> {

    private final List<ValueWithVariable> valueWithVariables;

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
    public boolean addAll(int index, @NotNull Collection<? extends ValueWithVariable> c) {
        return false;
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

    @Override
    public ValueWithVariable get(int index) {
        return valueWithVariables.get(index);
    }

    @Override
    public ValueWithVariable set(int index, ValueWithVariable element) {
        return valueWithVariables.set(index, element);
    }

    @Override
    public void add(int index, ValueWithVariable element) {
        valueWithVariables.add(index, element);
    }

    @Override
    public ValueWithVariable remove(int index) {
        return valueWithVariables.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return valueWithVariables.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return valueWithVariables.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<ValueWithVariable> listIterator() {
        return valueWithVariables.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<ValueWithVariable> listIterator(int index) {
        return valueWithVariables.listIterator(index);
    }

    @NotNull
    @Override
    public List<ValueWithVariable> subList(int fromIndex, int toIndex) {
        return valueWithVariables.subList(fromIndex, toIndex);
    }
}
