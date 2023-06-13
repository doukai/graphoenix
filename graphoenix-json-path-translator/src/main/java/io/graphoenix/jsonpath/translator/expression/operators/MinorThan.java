package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public class MinorThan extends ComparisonOperator {
    public MinorThan(String element, Expression expression) {
        super(element, "<", expression);
    }
}
