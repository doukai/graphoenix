package io.graphoenix.jsonpath.translator.expression;

import java.util.List;
import java.util.stream.Collectors;

public class MultiOrExpression implements Expression {

    private final List<Expression> expressionList;

    public MultiOrExpression(List<Expression> expressionList) {
        this.expressionList = expressionList;
    }

    @Override
    public String toString() {
        return expressionList.stream().map(Object::toString).collect(Collectors.joining(" || ", "(", ")"));
    }
}
