package io.graphoenix.jsonpath.expression.operators;

import io.graphoenix.jsonpath.expression.Expression;

public class GreaterThanEquals extends ComparisonOperator {
    public GreaterThanEquals(String element, Expression expression) {
        super(element, ">=", expression);
    }
}
