// Generated from GraphqlSDL.g4 by ANTLR 4.9
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link GraphqlSDLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface GraphqlSDLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#typeSystemDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeSystemDefinition(GraphqlSDLParser.TypeSystemDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#typeSystemExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeSystemExtension(GraphqlSDLParser.TypeSystemExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#schemaDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchemaDefinition(GraphqlSDLParser.SchemaDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#schemaExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchemaExtension(GraphqlSDLParser.SchemaExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#operationTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperationTypeDefinition(GraphqlSDLParser.OperationTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#typeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDefinition(GraphqlSDLParser.TypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#typeExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeExtension(GraphqlSDLParser.TypeExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#emptyParentheses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyParentheses(GraphqlSDLParser.EmptyParenthesesContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#scalarTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarTypeDefinition(GraphqlSDLParser.ScalarTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#scalarTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarTypeExtensionDefinition(GraphqlSDLParser.ScalarTypeExtensionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectTypeDefinition(GraphqlSDLParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#objectTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectTypeExtensionDefinition(GraphqlSDLParser.ObjectTypeExtensionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#implementsInterfaces}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImplementsInterfaces(GraphqlSDLParser.ImplementsInterfacesContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#fieldsDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldsDefinition(GraphqlSDLParser.FieldsDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#extensionFieldsDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtensionFieldsDefinition(GraphqlSDLParser.ExtensionFieldsDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#fieldDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDefinition(GraphqlSDLParser.FieldDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#argumentsDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentsDefinition(GraphqlSDLParser.ArgumentsDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#inputValueDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputValueDefinition(GraphqlSDLParser.InputValueDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#interfaceTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceTypeDefinition(GraphqlSDLParser.InterfaceTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#interfaceTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceTypeExtensionDefinition(GraphqlSDLParser.InterfaceTypeExtensionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#unionTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionTypeDefinition(GraphqlSDLParser.UnionTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#unionTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionTypeExtensionDefinition(GraphqlSDLParser.UnionTypeExtensionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#unionMembership}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionMembership(GraphqlSDLParser.UnionMembershipContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#unionMembers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionMembers(GraphqlSDLParser.UnionMembersContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumTypeDefinition(GraphqlSDLParser.EnumTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#enumTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumTypeExtensionDefinition(GraphqlSDLParser.EnumTypeExtensionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#enumValueDefinitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValueDefinitions(GraphqlSDLParser.EnumValueDefinitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#extensionEnumValueDefinitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtensionEnumValueDefinitions(GraphqlSDLParser.ExtensionEnumValueDefinitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#enumValueDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValueDefinition(GraphqlSDLParser.EnumValueDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#inputObjectTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputObjectTypeDefinition(GraphqlSDLParser.InputObjectTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#inputObjectTypeExtensionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputObjectTypeExtensionDefinition(GraphqlSDLParser.InputObjectTypeExtensionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#inputObjectValueDefinitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputObjectValueDefinitions(GraphqlSDLParser.InputObjectValueDefinitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#extensionInputObjectValueDefinitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtensionInputObjectValueDefinitions(GraphqlSDLParser.ExtensionInputObjectValueDefinitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#directiveDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectiveDefinition(GraphqlSDLParser.DirectiveDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#directiveLocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectiveLocation(GraphqlSDLParser.DirectiveLocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#directiveLocations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectiveLocations(GraphqlSDLParser.DirectiveLocationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#operationType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperationType(GraphqlSDLParser.OperationTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#description}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescription(GraphqlSDLParser.DescriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#enumValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValue(GraphqlSDLParser.EnumValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#arrayValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayValue(GraphqlSDLParser.ArrayValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayValueWithVariable(GraphqlSDLParser.ArrayValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#objectValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectValue(GraphqlSDLParser.ObjectValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectValueWithVariable(GraphqlSDLParser.ObjectValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#objectField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectField(GraphqlSDLParser.ObjectFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectFieldWithVariable(GraphqlSDLParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#directives}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectives(GraphqlSDLParser.DirectivesContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirective(GraphqlSDLParser.DirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(GraphqlSDLParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(GraphqlSDLParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#baseName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseName(GraphqlSDLParser.BaseNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#fragmentName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFragmentName(GraphqlSDLParser.FragmentNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#enumValueName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValueName(GraphqlSDLParser.EnumValueNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(GraphqlSDLParser.NameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(GraphqlSDLParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#valueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueWithVariable(GraphqlSDLParser.ValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(GraphqlSDLParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#defaultValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValue(GraphqlSDLParser.DefaultValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#stringValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringValue(GraphqlSDLParser.StringValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(GraphqlSDLParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(GraphqlSDLParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#listType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListType(GraphqlSDLParser.ListTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlSDLParser#nonNullType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonNullType(GraphqlSDLParser.NonNullTypeContext ctx);
}