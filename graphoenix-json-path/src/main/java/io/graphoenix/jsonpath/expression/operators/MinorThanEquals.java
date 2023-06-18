package io.graphoenix.jsonpath.expression.operators;

import io.graphoenix.jsonpath.expression.Expression;

public class MinorThanEquals extends ComparisonOperator {
    public MinorThanEquals(String element, Expression expression) {
        super(element, "<=", expression);
    }
}
