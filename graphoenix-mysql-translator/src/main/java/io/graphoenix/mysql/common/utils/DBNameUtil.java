package io.graphoenix.mysql.common.utils;

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

public enum DBNameUtil {

    DB_NAME_UTIL;

    public String graphqlTypeNameToTableName(String graphqlTypeName) {

        return nameToDBEscape(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlTypeName));
    }

    public String graphqlTypeNameToTableAliaName(String graphqlTypeName, int level) {

        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlTypeName) + "_" + level;
    }

    public String graphqlFieldNameToColumnName(String graphqlFieldName) {

        return nameToDBEscape(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlFieldName));
    }

    public String graphqlFieldNameToVariableName(String graphqlTypeName, String graphqlFieldName) {

        return String.join("_", CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlTypeName), CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlFieldName));
    }

    public String graphqlTypeToDBType(String graphqlType) {

        return nameToDBEscape(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, graphqlType));
    }

    public String directiveToTableOption(String argumentName, String argumentValue) {

        return String.format("%s=%s", CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, argumentName), stringValueToDBVarchar(CharMatcher.anyOf("\"").trimFrom(argumentValue)));
    }

    public String directiveTocColumnDefinition(String argumentName, String argumentValue) {

        return String.format("%s %s", CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, argumentName), stringValueToDBVarchar(CharMatcher.anyOf("\"").trimFrom(argumentValue)));
    }

    public String booleanDirectiveTocColumnDefinition(String argumentName) {

        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, argumentName);
    }

    public String graphqlDescriptionToDBComment(String description) {

        return stringValueToDBVarchar(CharMatcher.anyOf("\"").or(CharMatcher.anyOf("\"\"\"")).trimFrom(description));
    }

    public String graphqlStringValueToDBOption(String argumentName) {

        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, CharMatcher.anyOf("\"").trimFrom(argumentName));
    }

    public String stringValueToDBVarchar(String stringValue) {

        return String.format("'%s'", stringValue);
    }

    public String nameToDBEscape(String stringValue) {

        return String.format("`%s`", stringValue);
    }


    public Table dualTable() {
        return new Table("dual");
    }

    public Table typeToTable(String typeName) {
        return new Table(graphqlTypeNameToTableName(typeName));
    }

    public Table typeToTable(String typeName, int level) {
        Table table = new Table(graphqlTypeNameToTableName(typeName));
        table.setAlias(new Alias(graphqlTypeNameToTableAliaName(typeName, level)));
        return table;
    }

    public Table typeToTable(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new Table(graphqlTypeNameToTableName(objectTypeDefinitionContext.name().getText()));
    }

    public Column fieldToColumn(Table table, GraphqlParser.ArgumentContext argumentContext) {
        return new Column(table, graphqlFieldNameToColumnName(argumentContext.name().getText()));
    }

    public Column fieldToColumn(Table table, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return new Column(table, graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText()));
    }

    public Column fieldToColumn(Table table, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return new Column(table, graphqlFieldNameToColumnName(objectFieldWithVariableContext.name().getText()));
    }

    public Column fieldToColumn(Table table, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return new Column(table, graphqlFieldNameToColumnName(objectFieldContext.name().getText()));
    }

    public Column fieldToColumn(Table table, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new Column(table, graphqlFieldNameToColumnName(fieldDefinitionContext.name().getText()));
    }

    public Column fieldToColumn(Table table, String fieldName) {
        return new Column(table, graphqlFieldNameToColumnName(fieldName));
    }

    public Column fieldToColumn(String typeName, GraphqlParser.ArgumentContext argumentContext) {
        return new Column(new Table(graphqlTypeNameToTableName(typeName)), graphqlFieldNameToColumnName(argumentContext.name().getText()));
    }

    public Column fieldToColumn(String typeName, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return new Column(new Table(graphqlTypeNameToTableName(typeName)), graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText()));
    }

    public Column fieldToColumn(String typeName, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return new Column(new Table(graphqlTypeNameToTableName(typeName)), graphqlFieldNameToColumnName(objectFieldWithVariableContext.name().getText()));
    }

    public Column fieldToColumn(String typeName, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return new Column(new Table(graphqlTypeNameToTableName(typeName)), graphqlFieldNameToColumnName(objectFieldContext.name().getText()));
    }

    public Column fieldToColumn(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new Column(new Table(graphqlTypeNameToTableName(typeName)), graphqlFieldNameToColumnName(fieldDefinitionContext.name().getText()));
    }

    public Column fieldToColumn(String typeName, String fieldName) {
        return new Column(new Table(graphqlTypeNameToTableName(typeName)), graphqlFieldNameToColumnName(fieldName));
    }
}
