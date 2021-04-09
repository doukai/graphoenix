// Generated from GraphqlSDL.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link GraphqlSDLParser}.
 */
public interface GraphqlSDLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#typeSystemDefinition}.
	 * @param ctx the parse tree
	 */
	void enterTypeSystemDefinition(GraphqlSDLParser.TypeSystemDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#typeSystemDefinition}.
	 * @param ctx the parse tree
	 */
	void exitTypeSystemDefinition(GraphqlSDLParser.TypeSystemDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#typeSystemExtension}.
	 * @param ctx the parse tree
	 */
	void enterTypeSystemExtension(GraphqlSDLParser.TypeSystemExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#typeSystemExtension}.
	 * @param ctx the parse tree
	 */
	void exitTypeSystemExtension(GraphqlSDLParser.TypeSystemExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#schemaDefinition}.
	 * @param ctx the parse tree
	 */
	void enterSchemaDefinition(GraphqlSDLParser.SchemaDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#schemaDefinition}.
	 * @param ctx the parse tree
	 */
	void exitSchemaDefinition(GraphqlSDLParser.SchemaDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#schemaExtension}.
	 * @param ctx the parse tree
	 */
	void enterSchemaExtension(GraphqlSDLParser.SchemaExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#schemaExtension}.
	 * @param ctx the parse tree
	 */
	void exitSchemaExtension(GraphqlSDLParser.SchemaExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#operationTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterOperationTypeDefinition(GraphqlSDLParser.OperationTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#operationTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitOperationTypeDefinition(GraphqlSDLParser.OperationTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#typeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterTypeDefinition(GraphqlSDLParser.TypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#typeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitTypeDefinition(GraphqlSDLParser.TypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#typeExtension}.
	 * @param ctx the parse tree
	 */
	void enterTypeExtension(GraphqlSDLParser.TypeExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#typeExtension}.
	 * @param ctx the parse tree
	 */
	void exitTypeExtension(GraphqlSDLParser.TypeExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#emptyParentheses}.
	 * @param ctx the parse tree
	 */
	void enterEmptyParentheses(GraphqlSDLParser.EmptyParenthesesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#emptyParentheses}.
	 * @param ctx the parse tree
	 */
	void exitEmptyParentheses(GraphqlSDLParser.EmptyParenthesesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#scalarTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterScalarTypeDefinition(GraphqlSDLParser.ScalarTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#scalarTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitScalarTypeDefinition(GraphqlSDLParser.ScalarTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#scalarTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterScalarTypeExtensionDefinition(GraphqlSDLParser.ScalarTypeExtensionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#scalarTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitScalarTypeExtensionDefinition(GraphqlSDLParser.ScalarTypeExtensionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterObjectTypeDefinition(GraphqlSDLParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitObjectTypeDefinition(GraphqlSDLParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#objectTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterObjectTypeExtensionDefinition(GraphqlSDLParser.ObjectTypeExtensionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#objectTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitObjectTypeExtensionDefinition(GraphqlSDLParser.ObjectTypeExtensionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#implementsInterfaces}.
	 * @param ctx the parse tree
	 */
	void enterImplementsInterfaces(GraphqlSDLParser.ImplementsInterfacesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#implementsInterfaces}.
	 * @param ctx the parse tree
	 */
	void exitImplementsInterfaces(GraphqlSDLParser.ImplementsInterfacesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#fieldsDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFieldsDefinition(GraphqlSDLParser.FieldsDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#fieldsDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFieldsDefinition(GraphqlSDLParser.FieldsDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#extensionFieldsDefinition}.
	 * @param ctx the parse tree
	 */
	void enterExtensionFieldsDefinition(GraphqlSDLParser.ExtensionFieldsDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#extensionFieldsDefinition}.
	 * @param ctx the parse tree
	 */
	void exitExtensionFieldsDefinition(GraphqlSDLParser.ExtensionFieldsDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#fieldDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFieldDefinition(GraphqlSDLParser.FieldDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#fieldDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFieldDefinition(GraphqlSDLParser.FieldDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#argumentsDefinition}.
	 * @param ctx the parse tree
	 */
	void enterArgumentsDefinition(GraphqlSDLParser.ArgumentsDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#argumentsDefinition}.
	 * @param ctx the parse tree
	 */
	void exitArgumentsDefinition(GraphqlSDLParser.ArgumentsDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#inputValueDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInputValueDefinition(GraphqlSDLParser.InputValueDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#inputValueDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInputValueDefinition(GraphqlSDLParser.InputValueDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#interfaceTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceTypeDefinition(GraphqlSDLParser.InterfaceTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#interfaceTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceTypeDefinition(GraphqlSDLParser.InterfaceTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#interfaceTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceTypeExtensionDefinition(GraphqlSDLParser.InterfaceTypeExtensionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#interfaceTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceTypeExtensionDefinition(GraphqlSDLParser.InterfaceTypeExtensionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#unionTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterUnionTypeDefinition(GraphqlSDLParser.UnionTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#unionTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitUnionTypeDefinition(GraphqlSDLParser.UnionTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#unionTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterUnionTypeExtensionDefinition(GraphqlSDLParser.UnionTypeExtensionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#unionTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitUnionTypeExtensionDefinition(GraphqlSDLParser.UnionTypeExtensionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#unionMembership}.
	 * @param ctx the parse tree
	 */
	void enterUnionMembership(GraphqlSDLParser.UnionMembershipContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#unionMembership}.
	 * @param ctx the parse tree
	 */
	void exitUnionMembership(GraphqlSDLParser.UnionMembershipContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#unionMembers}.
	 * @param ctx the parse tree
	 */
	void enterUnionMembers(GraphqlSDLParser.UnionMembersContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#unionMembers}.
	 * @param ctx the parse tree
	 */
	void exitUnionMembers(GraphqlSDLParser.UnionMembersContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumTypeDefinition(GraphqlSDLParser.EnumTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumTypeDefinition(GraphqlSDLParser.EnumTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#enumTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumTypeExtensionDefinition(GraphqlSDLParser.EnumTypeExtensionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#enumTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumTypeExtensionDefinition(GraphqlSDLParser.EnumTypeExtensionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#enumValueDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterEnumValueDefinitions(GraphqlSDLParser.EnumValueDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#enumValueDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitEnumValueDefinitions(GraphqlSDLParser.EnumValueDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#extensionEnumValueDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterExtensionEnumValueDefinitions(GraphqlSDLParser.ExtensionEnumValueDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#extensionEnumValueDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitExtensionEnumValueDefinitions(GraphqlSDLParser.ExtensionEnumValueDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#enumValueDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumValueDefinition(GraphqlSDLParser.EnumValueDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#enumValueDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumValueDefinition(GraphqlSDLParser.EnumValueDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#inputObjectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInputObjectTypeDefinition(GraphqlSDLParser.InputObjectTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#inputObjectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInputObjectTypeDefinition(GraphqlSDLParser.InputObjectTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#inputObjectTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInputObjectTypeExtensionDefinition(GraphqlSDLParser.InputObjectTypeExtensionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#inputObjectTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInputObjectTypeExtensionDefinition(GraphqlSDLParser.InputObjectTypeExtensionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#inputObjectValueDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterInputObjectValueDefinitions(GraphqlSDLParser.InputObjectValueDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#inputObjectValueDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitInputObjectValueDefinitions(GraphqlSDLParser.InputObjectValueDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#extensionInputObjectValueDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterExtensionInputObjectValueDefinitions(GraphqlSDLParser.ExtensionInputObjectValueDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#extensionInputObjectValueDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitExtensionInputObjectValueDefinitions(GraphqlSDLParser.ExtensionInputObjectValueDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#directiveDefinition}.
	 * @param ctx the parse tree
	 */
	void enterDirectiveDefinition(GraphqlSDLParser.DirectiveDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#directiveDefinition}.
	 * @param ctx the parse tree
	 */
	void exitDirectiveDefinition(GraphqlSDLParser.DirectiveDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#directiveLocation}.
	 * @param ctx the parse tree
	 */
	void enterDirectiveLocation(GraphqlSDLParser.DirectiveLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#directiveLocation}.
	 * @param ctx the parse tree
	 */
	void exitDirectiveLocation(GraphqlSDLParser.DirectiveLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#directiveLocations}.
	 * @param ctx the parse tree
	 */
	void enterDirectiveLocations(GraphqlSDLParser.DirectiveLocationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#directiveLocations}.
	 * @param ctx the parse tree
	 */
	void exitDirectiveLocations(GraphqlSDLParser.DirectiveLocationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#operationType}.
	 * @param ctx the parse tree
	 */
	void enterOperationType(GraphqlSDLParser.OperationTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#operationType}.
	 * @param ctx the parse tree
	 */
	void exitOperationType(GraphqlSDLParser.OperationTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#description}.
	 * @param ctx the parse tree
	 */
	void enterDescription(GraphqlSDLParser.DescriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#description}.
	 * @param ctx the parse tree
	 */
	void exitDescription(GraphqlSDLParser.DescriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#enumValue}.
	 * @param ctx the parse tree
	 */
	void enterEnumValue(GraphqlSDLParser.EnumValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#enumValue}.
	 * @param ctx the parse tree
	 */
	void exitEnumValue(GraphqlSDLParser.EnumValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#arrayValue}.
	 * @param ctx the parse tree
	 */
	void enterArrayValue(GraphqlSDLParser.ArrayValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#arrayValue}.
	 * @param ctx the parse tree
	 */
	void exitArrayValue(GraphqlSDLParser.ArrayValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterArrayValueWithVariable(GraphqlSDLParser.ArrayValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitArrayValueWithVariable(GraphqlSDLParser.ArrayValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#objectValue}.
	 * @param ctx the parse tree
	 */
	void enterObjectValue(GraphqlSDLParser.ObjectValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#objectValue}.
	 * @param ctx the parse tree
	 */
	void exitObjectValue(GraphqlSDLParser.ObjectValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterObjectValueWithVariable(GraphqlSDLParser.ObjectValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitObjectValueWithVariable(GraphqlSDLParser.ObjectValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#objectField}.
	 * @param ctx the parse tree
	 */
	void enterObjectField(GraphqlSDLParser.ObjectFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#objectField}.
	 * @param ctx the parse tree
	 */
	void exitObjectField(GraphqlSDLParser.ObjectFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterObjectFieldWithVariable(GraphqlSDLParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitObjectFieldWithVariable(GraphqlSDLParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#directives}.
	 * @param ctx the parse tree
	 */
	void enterDirectives(GraphqlSDLParser.DirectivesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#directives}.
	 * @param ctx the parse tree
	 */
	void exitDirectives(GraphqlSDLParser.DirectivesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterDirective(GraphqlSDLParser.DirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitDirective(GraphqlSDLParser.DirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(GraphqlSDLParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(GraphqlSDLParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(GraphqlSDLParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(GraphqlSDLParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#baseName}.
	 * @param ctx the parse tree
	 */
	void enterBaseName(GraphqlSDLParser.BaseNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#baseName}.
	 * @param ctx the parse tree
	 */
	void exitBaseName(GraphqlSDLParser.BaseNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void enterFragmentName(GraphqlSDLParser.FragmentNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void exitFragmentName(GraphqlSDLParser.FragmentNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#enumValueName}.
	 * @param ctx the parse tree
	 */
	void enterEnumValueName(GraphqlSDLParser.EnumValueNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#enumValueName}.
	 * @param ctx the parse tree
	 */
	void exitEnumValueName(GraphqlSDLParser.EnumValueNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(GraphqlSDLParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(GraphqlSDLParser.NameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(GraphqlSDLParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(GraphqlSDLParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#valueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterValueWithVariable(GraphqlSDLParser.ValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#valueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitValueWithVariable(GraphqlSDLParser.ValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(GraphqlSDLParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(GraphqlSDLParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(GraphqlSDLParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(GraphqlSDLParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#stringValue}.
	 * @param ctx the parse tree
	 */
	void enterStringValue(GraphqlSDLParser.StringValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#stringValue}.
	 * @param ctx the parse tree
	 */
	void exitStringValue(GraphqlSDLParser.StringValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(GraphqlSDLParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(GraphqlSDLParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(GraphqlSDLParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(GraphqlSDLParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#listType}.
	 * @param ctx the parse tree
	 */
	void enterListType(GraphqlSDLParser.ListTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#listType}.
	 * @param ctx the parse tree
	 */
	void exitListType(GraphqlSDLParser.ListTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlSDLParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void enterNonNullType(GraphqlSDLParser.NonNullTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlSDLParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void exitNonNullType(GraphqlSDLParser.NonNullTypeContext ctx);
}