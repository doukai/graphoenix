package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public class NotEqualsTo extends ComparisonOperator {
    public NotEqualsTo(String element, Expression expression) {
        super(element, "!=", expression);
    }
}
