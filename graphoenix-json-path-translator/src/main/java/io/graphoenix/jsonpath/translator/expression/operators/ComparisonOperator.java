package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public abstract class ComparisonOperator implements Expression {

    private final String element;

    private final String operator;

    private final Expression expression;

    private final boolean not;

    public ComparisonOperator(String element, String operator, Expression expression) {
        this.element = element;
        this.operator = operator;
        this.expression = expression;
        this.not = false;
    }

    public ComparisonOperator(String element, String operator, Expression expression, boolean not) {
        this.element = element;
        this.operator = operator;
        this.expression = expression;
        this.not = not;
    }

    @Override
    public String toString() {
        String comparisonOperator = "@." + element + " " + operator + " " + expression;
        if (not) {
            return "!(" + comparisonOperator + ")";
        } else {
            return comparisonOperator;
        }
    }
}
