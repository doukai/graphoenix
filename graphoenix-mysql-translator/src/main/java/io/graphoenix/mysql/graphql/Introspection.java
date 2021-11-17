package io.graphoenix.mysql.graphql;

public class Introspection {
    public static final String gql = "type __Schema {\n" +
            "    id: ID @column(autoIncrement: true)\n" +
            "    queryTypeName: String\n" +
            "    mutationTypeName: String\n" +
            "    subscriptionTypeName: String\n" +
            "    types: [__Type!]! @map(from: \"id\", to: \"schemaId\")\n" +
            "    queryType: __Type! @map(from: \"queryTypeName\", to: \"name\")\n" +
            "    mutationType: __Type @map(from: \"mutationTypeName\", to: \"name\")\n" +
            "    subscriptionType: __Type @map(from: \"subscriptionTypeName\", to: \"name\")\n" +
            "    directives: [__Directive!]! @map(from: \"id\", to: \"schemaId\")\n" +
            "}\n" +
            "type __Type {\n" +
            "    name: ID! @dataType(type: \"varchar\")\n" +
            "    schemaId: Int\n" +
            "    kind: __TypeKind!\n" +
            "    description: String\n" +
            "    # OBJECT and INTERFACE only\n" +
            "    fields: [__Field!] @map(from: \"name\", to: \"ofTypeName\")\n" +
            "    # OBJECT only\n" +
            "    interfaces: [__Type!] @map(from: \"name\", with: {type: \"__TypeInterfaces\", from: \"typeName\", to: \"interfaceName\"}, to: \"name\")\n" +
            "    # INTERFACE and UNION only\n" +
            "    possibleTypes: [__Type!] @map(from: \"name\", with: {type: \"__TypePossibleTypes\", from: \"typeName\", to: \"possibleTypeName\"}, to: \"name\")\n" +
            "    # ENUM only\n" +
            "    enumValues: [__EnumValue!] @map(from: \"name\", to: \"ofTypeName\")\n" +
            "    # INPUT_OBJECT only\n" +
            "    inputFields: [__InputValue!] @map(from: \"name\", to: \"ofTypeName\")\n" +
            "    # NON_NULL and LIST only\n" +
            "    ofTypeName: String\n" +
            "    ofType: __Type @map(from: \"ofTypeName\", to: \"name\")\n" +
            "}\n" +
            "type __TypeInterfaces {\n" +
            "    id: ID @column(autoIncrement: true)\n" +
            "    typeName: String!\n" +
            "    interfaceName: String!\n" +
            "}\n" +
            "type __TypePossibleTypes {\n" +
            "    id: ID @column(autoIncrement: true)\n" +
            "    typeName: String!\n" +
            "    possibleTypeName: String!\n" +
            "}\n" +
            "type __Field {\n" +
            "    id: ID @column(autoIncrement: true)\n" +
            "    name: String\n" +
            "    typeName: String\n" +
            "    ofTypeName: String\n" +
            "    description: String\n" +
            "    args: [__InputValue!]! @map(from: \"id\", to: \"fieldId\")\n" +
            "    type: __Type! @map(from: \"typeName\", to: \"name\")\n" +
            "    deprecationReason: String\n" +
            "}\n" +
            "type __InputValue {\n" +
            "    id: ID @column(autoIncrement: true)\n" +
            "    name: String\n" +
            "    typeName: String\n" +
            "    ofTypeName: String\n" +
            "    fieldId: Int\n" +
            "    directiveName: String\n" +
            "    description: String\n" +
            "    type: __Type! @map(from: \"typeName\", to: \"name\")\n" +
            "    defaultValue: String\n" +
            "}\n" +
            "type __EnumValue {\n" +
            "    id: ID @column(autoIncrement: true)\n" +
            "    name: String\n" +
            "    ofTypeName: String\n" +
            "    description: String\n" +
            "    deprecationReason: String\n" +
            "}\n" +
            "enum __TypeKind {\n" +
            "    SCALAR\n" +
            "    OBJECT\n" +
            "    INTERFACE\n" +
            "    UNION\n" +
            "    ENUM\n" +
            "    INPUT_OBJECT\n" +
            "    LIST\n" +
            "    NON_NULL\n" +
            "}\n" +
            "type __Directive {\n" +
            "    name: ID! @dataType(type: \"varchar\")\n" +
            "    schemaId: Int\n" +
            "    description: String\n" +
            "    locations: [__DirectiveLocation!]! @map(from: \"name\", with: {type: \"__DirectiveLocations\", from: \"directiveName\", to: \"directiveLocation\"})\n" +
            "    args: [__InputValue!]! @map(from: \"name\", to: \"directiveName\")\n" +
            "    onOperation: Boolean\n" +
            "    onFragment: Boolean\n" +
            "    onField: Boolean\n" +
            "}\n" +
            "type __DirectiveLocations {\n" +
            "    id: ID @column(autoIncrement: true)\n" +
            "    directiveName: String!\n" +
            "    directiveLocation: __DirectiveLocation!\n" +
            "}\n" +
            "enum __DirectiveLocation {\n" +
            "    QUERY\n" +
            "    MUTATION\n" +
            "    SUBSCRIPTION\n" +
            "    FIELD\n" +
            "    FRAGMENT_DEFINITION\n" +
            "    FRAGMENT_SPREAD\n" +
            "    INLINE_FRAGMENT\n" +
            "    SCHEMA\n" +
            "    SCALAR\n" +
            "    OBJECT\n" +
            "    FIELD_DEFINITION\n" +
            "    ARGUMENT_DEFINITION\n" +
            "    INTERFACE\n" +
            "    UNION\n" +
            "    ENUM\n" +
            "    ENUM_VALUE\n" +
            "    INPUT_OBJECT\n" +
            "    INPUT_FIELD_DEFINITION\n" +
            "}\n";
}
