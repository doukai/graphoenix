package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public class GreaterThanEquals extends ComparisonOperator {
    public GreaterThanEquals(String element, Expression expression) {
        super(element, ">=", expression);
    }
}
