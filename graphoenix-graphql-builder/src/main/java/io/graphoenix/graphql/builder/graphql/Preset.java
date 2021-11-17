package io.graphoenix.graphql.builder.graphql;

public class Preset {
    public static final String gql = "scalar ID\n" +
            "scalar Int\n" +
            "scalar Float\n" +
            "scalar String\n" +
            "scalar Boolean\n" +
            "scalar Booleaninterface Meta {\n" +
            "    version: Int\n" +
            "    isDeprecated: Boolean\n" +
            "}\n" +
            "enum Operator {\n" +
            "    EQ\n" +
            "    NEQ\n" +
            "    LK\n" +
            "    NLK\n" +
            "    GT\n" +
            "    NLTE\n" +
            "    GTE\n" +
            "    NLT\n" +
            "    LT\n" +
            "    NGTE\n" +
            "    LTE\n" +
            "    NGT\n" +
            "    NIL\n" +
            "    NNIL\n" +
            "}\n" +
            "enum Conditional {\n" +
            "    AND\n" +
            "    OR\n" +
            "}\n" +
            "input IDExpression {\n" +
            "    opr: Operator = EQ\n" +
            "    val: ID\n" +
            "    in: [ID]\n" +
            "}\n" +
            "input StringExpression {\n" +
            "    opr: Operator = EQ\n" +
            "    val: String\n" +
            "    in: [String]\n" +
            "}\n" +
            "input IntExpression {\n" +
            "    opr: Operator = EQ\n" +
            "    val: Int\n" +
            "    in: [Int]\n" +
            "}\n" +
            "input FloatExpression {\n" +
            "    opr: Operator = EQ\n" +
            "    val: Float\n" +
            "    in: [Float]\n" +
            "}\n" +
            "input MapWith {\n" +
            "    type: String\n" +
            "    from: String\n" +
            "    to: String\n" +
            "}\n" +
            "directive @dataType(\n" +
            "    type: String\n" +
            "    length: Int\n" +
            "    decimals: Int\n" +
            ") on FIELD_DEFINITION\n" +
            "directive @map(\n" +
            "    from: String\n" +
            "    with: MapWith\n" +
            "    to: String\n" +
            ") on FIELD_DEFINITION\n";
}
