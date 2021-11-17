package io.graphoenix.mysql.graphql;

public class Preset {
    public static final String gql = "directive @table(\n" +
            "    engine : String = \"InnoDB\"\n" +
            ") on OBJECT\n" +
            "directive @column(\n" +
            "    default: String\n" +
            "    autoIncrement: Boolean\n" +
            ") on FIELD_DEFINITION";
}
