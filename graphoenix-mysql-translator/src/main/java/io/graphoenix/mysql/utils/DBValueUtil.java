package io.graphoenix.mysql.utils;

import com.google.common.base.CharMatcher;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLProblem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.SetStatement;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Arrays;

@ApplicationScoped
public class DBValueUtil {

    private final DBNameUtil dbNameUtil;

    @Inject
    public DBValueUtil(DBNameUtil dbNameUtil) {
        this.dbNameUtil = dbNameUtil;
    }

    public Expression scalarValueToDBValue(GraphqlParser.ValueContext valueContext) {
        return scalarValueToDBValue(valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue());
    }

    public Expression scalarValueWithVariableToDBValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.variable() != null) {
            return variableToJdbcNamedParameter(valueWithVariableContext.variable());
        }

        return scalarValueToDBValue(valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue());
    }

    public Expression objectFieldVariableToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        JdbcNamedParameter jdbcNamedParameter = new JdbcNamedParameter();
        jdbcNamedParameter.setName(valueWithVariableContext.variable().name().getText());

        Function function = new Function();
        function.setName("JSON_EXTRACT");
        function.setParameters(new ExpressionList(Arrays.asList(jdbcNamedParameter, new StringValue("$." + inputValueDefinitionContext.name().getText()))));
        return function;
    }

    public Expression enumValueToDBValue(GraphqlParser.ValueContext valueContext) {
        return new StringValue(valueContext.enumValue().enumValueName().getText());
    }

    public Expression enumValueWithVariableToDBValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.variable() != null) {
            return variableToJdbcNamedParameter(valueWithVariableContext.variable());
        }
        return new StringValue(valueWithVariableContext.enumValue().enumValueName().getText());
    }

    public Expression scalarValueToDBValue(TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        if (stringValue != null) {
            return new StringValue(CharMatcher.is('"').trimFrom(stringValue.getText()));
        } else if (intValue != null) {
            return new LongValue(intValue.getText());
        } else if (floatValue != null) {
            return new DoubleValue(floatValue.getText());
        } else if (booleanValue != null) {
            throw new GraphQLProblem(GraphQLErrorType.UNKNOWN);
        } else if (nullValue != null) {
            return new NullValue();
        }
        return null;
    }

    public JdbcNamedParameter variableToJdbcNamedParameter(GraphqlParser.VariableContext variableContext) {
        JdbcNamedParameter jdbcNamedParameter = new JdbcNamedParameter();
        jdbcNamedParameter.setName(variableContext.name().getText());
        return jdbcNamedParameter;
    }

    public SetStatement createInsertIdSetStatement(String typeName, String idFieldName, int level, int index) {
        String idVariableName = "@" + dbNameUtil.graphqlFieldNameToVariableName(typeName, idFieldName) + "_" + level + "_" + index;
        Function function = new Function();
        function.setName("LAST_INSERT_ID");
        return new SetStatement(idVariableName, function);
    }

    public UserVariable createInsertIdUserVariable(String typeName, String idFieldName, int level, int index) {
        String idVariableName = dbNameUtil.graphqlFieldNameToVariableName(typeName, idFieldName) + "_" + level + "_" + index;
        UserVariable userVariable = new UserVariable();
        userVariable.setName(idVariableName);
        return userVariable;
    }

    public Expression createIdValueExpression(GraphqlParser.ArgumentContext idArgumentContext) {
        return scalarValueWithVariableToDBValue(idArgumentContext.valueWithVariable());
    }

    public Expression createIdValueExpression(GraphqlParser.ObjectFieldWithVariableContext objectIdFieldWithVariableContext) {
        return scalarValueWithVariableToDBValue(objectIdFieldWithVariableContext.valueWithVariable());
    }

    public Expression createIdValueExpression(GraphqlParser.ObjectFieldContext objectIdFieldContext) {
        return scalarValueToDBValue(objectIdFieldContext.value());
    }

    public Expression valueWithVariableToDBValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.enumValue() != null) {
            return enumValueWithVariableToDBValue(valueWithVariableContext);
        } else {
            return scalarValueWithVariableToDBValue(valueWithVariableContext);
        }
    }

    public Expression valueToDBValue(GraphqlParser.ValueContext valueContext) {
        if (valueContext.enumValue() != null) {
            return enumValueToDBValue(valueContext);
        } else {
            return scalarValueToDBValue(valueContext);
        }
    }
}
