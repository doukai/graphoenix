package io.graphoenix.mysql.common.utils;

import com.google.common.base.CharMatcher;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.statement.SetStatement;
import org.antlr.v4.runtime.tree.TerminalNode;

import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;

public enum DBValueUtil {

    DB_VALUE_UTIL;

    public Expression scalarValueToDBValue(GraphqlParser.ValueContext valueContext) {
        return scalarValueToDBValue(valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue());
    }

    public Expression scalarValueWithVariableToDBValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        return scalarValueToDBValue(valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue());
    }

    public Expression scalarValueToDBValue(TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        if (stringValue != null) {
            return new StringValue(CharMatcher.is('"').trimFrom(stringValue.getText()));
        } else if (intValue != null) {
            return new LongValue(intValue.getText());
        } else if (floatValue != null) {
            return new DoubleValue(floatValue.getText());
        } else if (booleanValue != null) {
            //todo
        } else if (nullValue != null) {
            return new NullValue();
        }
        return null;
    }

    public SetStatement createInsertIdSetStatement(String typeName, String idFieldName) {
        String idVariableName = "@" + DB_NAME_UTIL.graphqlFieldNameToVariableName(typeName, idFieldName);
        Function function = new Function();
        function.setName("LAST_INSERT_ID");
        return new SetStatement(idVariableName, function);
    }

    public UserVariable createInsertIdUserVariable(String typeName, String idFieldName) {
        String idVariableName = DB_NAME_UTIL.graphqlFieldNameToVariableName(typeName, idFieldName);
        UserVariable userVariable = new UserVariable();
        userVariable.setName(idVariableName);
        return userVariable;
    }
}
