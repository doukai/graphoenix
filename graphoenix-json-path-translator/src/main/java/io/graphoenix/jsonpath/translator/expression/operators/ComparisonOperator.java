package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public abstract class ComparisonOperator implements Expression {

    private final String element;

    private final String operator;

    private final Expression expression;

    public ComparisonOperator(String element, String operator, Expression expression) {
        this.element = element;
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "@." + element + " " + operator + " " + expression;
    }
}
