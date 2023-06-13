package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.Expression;

public class NotInExpression extends ComparisonOperator {
    public NotInExpression(String element, Expression expression) {
        super(element, "nin", expression);
    }


}
