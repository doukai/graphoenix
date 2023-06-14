package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.ArrayValue;
import io.graphoenix.jsonpath.translator.expression.Expression;

import java.util.List;

public class InExpression extends ComparisonOperator {
    public InExpression(String element, Expression expression) {
        super(element, "in", expression);
    }

    public InExpression(String element, Expression... expressions) {
        super(element, "in", new ArrayValue(expressions));
    }

    public InExpression(String element, List<Expression> expressionList) {
        super(element, "in", new ArrayValue(expressionList));
    }
}
