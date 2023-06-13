package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public class EqualsTo extends ComparisonOperator {
    public EqualsTo(String element, Expression expression) {
        super(element, "==", expression);
    }
}
