package io.graphoenix.jsonpath.translator.expression;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayValue implements Expression {

    private final List<Expression> expressionList;

    public ArrayValue(List<Expression> expressionList) {
        this.expressionList = expressionList;
    }

    public ArrayValue(Expression... expressions) {
        this.expressionList = Arrays.asList(expressions);
    }

    @Override
    public String toString() {
        return expressionList.stream().map(Object::toString).collect(Collectors.joining(" , ", "[", "]"));
    }
}
