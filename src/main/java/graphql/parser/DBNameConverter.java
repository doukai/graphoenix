package graphql.parser;

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;

public enum DBNameConverter {

    INSTANCE;

    public String graphqlTypeNameToTableName(String graphqlTypeName) {

        return nameToDBEscape(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlTypeName));
    }

    public String graphqlTypeFieldNameToColumnName(String graphqlTypeName) {

        return nameToDBEscape(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlTypeName));
    }

    public String graphqlTypeToDBType(String graphqlTypeName) {

        return nameToDBEscape(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, graphqlTypeName));
    }

    public String directiveToTableOption(String argumentName, String argumentValue) {

        return String.format("`%s=%s`", CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, argumentName), argumentValue);
    }

    public String directiveTocColumnDefinition(String argumentName, String argumentValue) {

        return String.format("`%s %s`", CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, argumentName), argumentValue);
    }

    public String graphqlTypeFieldNameToRelationTableName(String graphqlTypeName, String graphqlFieldName) {

        return nameToDBEscape(Joiner.on("_").join(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlTypeName), CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlFieldName)));
    }

    public String graphqlTypeFieldNameToRelationColumnName(String graphqlFieldName, String graphqlIdFieldName) {

        return nameToDBEscape(Joiner.on("_").join(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlFieldName), CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, graphqlIdFieldName)));
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
