package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public class GreaterThan extends ComparisonOperator {
    public GreaterThan(String element, Expression expression) {
        super(element, ">", expression);
    }
}
