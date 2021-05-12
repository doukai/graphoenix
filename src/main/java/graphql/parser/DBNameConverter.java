package graphql.parser;

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;

public enum DBNameConverter {

    INSTANCE;

    public String graphqlTypeNameToTableName(String graphqlTypeName) {

        return nameToDBEscape(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlTypeName));
    }

    public String graphqlFieldNameToColumnName(String graphqlFieldName) {

        return nameToDBEscape(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlFieldName));
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

    public String graphqlDescriptionToDBComment(String description) {

        return stringValueToDBVarchar(CharMatcher.anyOf("\"").or(CharMatcher.anyOf("\"\"\"")).trimFrom(description));
    }

    public String stringValueToDBVarchar(String stringValue) {

        return String.format("'%s'", stringValue);
    }

    public String nameToDBEscape(String stringValue) {

        return String.format("`%s`", stringValue);
    }
}
