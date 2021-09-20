package io.graphoenix.mysql.common.utils;

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;

public enum DBNameUtil {

    DB_NAME_UTIL;

    public String graphqlTypeNameToTableName(String graphqlTypeName) {

        return nameToDBEscape(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlTypeName));
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
}
