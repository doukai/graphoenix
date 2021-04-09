// Generated from GraphqlOperation.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link GraphqlOperationParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface GraphqlOperationVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#operationDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperationDefinition(GraphqlOperationParser.OperationDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#variableDefinitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDefinitions(GraphqlOperationParser.VariableDefinitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#variableDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDefinition(GraphqlOperationParser.VariableDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#selectionSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectionSet(GraphqlOperationParser.SelectionSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#selection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelection(GraphqlOperationParser.SelectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(GraphqlOperationParser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlias(GraphqlOperationParser.AliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#fragmentSpread}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFragmentSpread(GraphqlOperationParser.FragmentSpreadContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#inlineFragment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineFragment(GraphqlOperationParser.InlineFragmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#fragmentDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFragmentDefinition(GraphqlOperationParser.FragmentDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#typeCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeCondition(GraphqlOperationParser.TypeConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#operationType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperationType(GraphqlOperationParser.OperationTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#description}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescription(GraphqlOperationParser.DescriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#enumValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValue(GraphqlOperationParser.EnumValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#arrayValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayValue(GraphqlOperationParser.ArrayValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayValueWithVariable(GraphqlOperationParser.ArrayValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#objectValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectValue(GraphqlOperationParser.ObjectValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectValueWithVariable(GraphqlOperationParser.ObjectValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#objectField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectField(GraphqlOperationParser.ObjectFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectFieldWithVariable(GraphqlOperationParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#directives}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectives(GraphqlOperationParser.DirectivesContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirective(GraphqlOperationParser.DirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(GraphqlOperationParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(GraphqlOperationParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#baseName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseName(GraphqlOperationParser.BaseNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#fragmentName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFragmentName(GraphqlOperationParser.FragmentNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#enumValueName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValueName(GraphqlOperationParser.EnumValueNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(GraphqlOperationParser.NameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(GraphqlOperationParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#valueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueWithVariable(GraphqlOperationParser.ValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(GraphqlOperationParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#defaultValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValue(GraphqlOperationParser.DefaultValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#stringValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringValue(GraphqlOperationParser.StringValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(GraphqlOperationParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(GraphqlOperationParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#listType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListType(GraphqlOperationParser.ListTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlOperationParser#nonNullType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonNullType(GraphqlOperationParser.NonNullTypeContext ctx);
}