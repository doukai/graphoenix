package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public class InExpression extends ComparisonOperator {
    public InExpression(String element, Expression expression) {
        super(element, "in", expression);
    }
}
