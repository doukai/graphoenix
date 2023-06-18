package io.graphoenix.jsonpath.expression.operators;

import io.graphoenix.jsonpath.expression.Expression;

public class MinorThan extends ComparisonOperator {
    public MinorThan(String element, Expression expression) {
        super(element, "<", expression);
    }
}
