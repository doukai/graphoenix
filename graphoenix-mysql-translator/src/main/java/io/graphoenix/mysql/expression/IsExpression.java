package io.graphoenix.mysql.expression;

import net.sf.jsqlparser.expression.operators.relational.EqualsTo;

public class IsExpression extends EqualsTo {

    @Override
    public String getStringExpression() {
        return "IS";
    }
}
