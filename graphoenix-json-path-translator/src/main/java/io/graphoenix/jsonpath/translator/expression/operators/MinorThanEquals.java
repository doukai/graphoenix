package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public class MinorThanEquals extends ComparisonOperator {
    public MinorThanEquals(String element, Expression expression) {
        super(element, "<=", expression);
    }
}
