package io.graphoenix.jsonpath.expression.operators;

import io.graphoenix.jsonpath.expression.Expression;

public class NotEqualsTo extends ComparisonOperator {
    public NotEqualsTo(String element, Expression expression) {
        super(element, "!=", expression);
    }
}
