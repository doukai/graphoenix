// Generated from GraphqlOperation.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link GraphqlOperationParser}.
 */
public interface GraphqlOperationListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#operationDefinition}.
	 * @param ctx the parse tree
	 */
	void enterOperationDefinition(GraphqlOperationParser.OperationDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#operationDefinition}.
	 * @param ctx the parse tree
	 */
	void exitOperationDefinition(GraphqlOperationParser.OperationDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#variableDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterVariableDefinitions(GraphqlOperationParser.VariableDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#variableDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitVariableDefinitions(GraphqlOperationParser.VariableDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#variableDefinition}.
	 * @param ctx the parse tree
	 */
	void enterVariableDefinition(GraphqlOperationParser.VariableDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#variableDefinition}.
	 * @param ctx the parse tree
	 */
	void exitVariableDefinition(GraphqlOperationParser.VariableDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#selectionSet}.
	 * @param ctx the parse tree
	 */
	void enterSelectionSet(GraphqlOperationParser.SelectionSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#selectionSet}.
	 * @param ctx the parse tree
	 */
	void exitSelectionSet(GraphqlOperationParser.SelectionSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#selection}.
	 * @param ctx the parse tree
	 */
	void enterSelection(GraphqlOperationParser.SelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#selection}.
	 * @param ctx the parse tree
	 */
	void exitSelection(GraphqlOperationParser.SelectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(GraphqlOperationParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(GraphqlOperationParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#alias}.
	 * @param ctx the parse tree
	 */
	void enterAlias(GraphqlOperationParser.AliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#alias}.
	 * @param ctx the parse tree
	 */
	void exitAlias(GraphqlOperationParser.AliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#fragmentSpread}.
	 * @param ctx the parse tree
	 */
	void enterFragmentSpread(GraphqlOperationParser.FragmentSpreadContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#fragmentSpread}.
	 * @param ctx the parse tree
	 */
	void exitFragmentSpread(GraphqlOperationParser.FragmentSpreadContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#inlineFragment}.
	 * @param ctx the parse tree
	 */
	void enterInlineFragment(GraphqlOperationParser.InlineFragmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#inlineFragment}.
	 * @param ctx the parse tree
	 */
	void exitInlineFragment(GraphqlOperationParser.InlineFragmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#fragmentDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFragmentDefinition(GraphqlOperationParser.FragmentDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#fragmentDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFragmentDefinition(GraphqlOperationParser.FragmentDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#typeCondition}.
	 * @param ctx the parse tree
	 */
	void enterTypeCondition(GraphqlOperationParser.TypeConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#typeCondition}.
	 * @param ctx the parse tree
	 */
	void exitTypeCondition(GraphqlOperationParser.TypeConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#operationType}.
	 * @param ctx the parse tree
	 */
	void enterOperationType(GraphqlOperationParser.OperationTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#operationType}.
	 * @param ctx the parse tree
	 */
	void exitOperationType(GraphqlOperationParser.OperationTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#description}.
	 * @param ctx the parse tree
	 */
	void enterDescription(GraphqlOperationParser.DescriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#description}.
	 * @param ctx the parse tree
	 */
	void exitDescription(GraphqlOperationParser.DescriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#enumValue}.
	 * @param ctx the parse tree
	 */
	void enterEnumValue(GraphqlOperationParser.EnumValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#enumValue}.
	 * @param ctx the parse tree
	 */
	void exitEnumValue(GraphqlOperationParser.EnumValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#arrayValue}.
	 * @param ctx the parse tree
	 */
	void enterArrayValue(GraphqlOperationParser.ArrayValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#arrayValue}.
	 * @param ctx the parse tree
	 */
	void exitArrayValue(GraphqlOperationParser.ArrayValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterArrayValueWithVariable(GraphqlOperationParser.ArrayValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitArrayValueWithVariable(GraphqlOperationParser.ArrayValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#objectValue}.
	 * @param ctx the parse tree
	 */
	void enterObjectValue(GraphqlOperationParser.ObjectValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#objectValue}.
	 * @param ctx the parse tree
	 */
	void exitObjectValue(GraphqlOperationParser.ObjectValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterObjectValueWithVariable(GraphqlOperationParser.ObjectValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitObjectValueWithVariable(GraphqlOperationParser.ObjectValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#objectField}.
	 * @param ctx the parse tree
	 */
	void enterObjectField(GraphqlOperationParser.ObjectFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#objectField}.
	 * @param ctx the parse tree
	 */
	void exitObjectField(GraphqlOperationParser.ObjectFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterObjectFieldWithVariable(GraphqlOperationParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitObjectFieldWithVariable(GraphqlOperationParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#directives}.
	 * @param ctx the parse tree
	 */
	void enterDirectives(GraphqlOperationParser.DirectivesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#directives}.
	 * @param ctx the parse tree
	 */
	void exitDirectives(GraphqlOperationParser.DirectivesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterDirective(GraphqlOperationParser.DirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitDirective(GraphqlOperationParser.DirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(GraphqlOperationParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(GraphqlOperationParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(GraphqlOperationParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(GraphqlOperationParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#baseName}.
	 * @param ctx the parse tree
	 */
	void enterBaseName(GraphqlOperationParser.BaseNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#baseName}.
	 * @param ctx the parse tree
	 */
	void exitBaseName(GraphqlOperationParser.BaseNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void enterFragmentName(GraphqlOperationParser.FragmentNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void exitFragmentName(GraphqlOperationParser.FragmentNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#enumValueName}.
	 * @param ctx the parse tree
	 */
	void enterEnumValueName(GraphqlOperationParser.EnumValueNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#enumValueName}.
	 * @param ctx the parse tree
	 */
	void exitEnumValueName(GraphqlOperationParser.EnumValueNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(GraphqlOperationParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(GraphqlOperationParser.NameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(GraphqlOperationParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(GraphqlOperationParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#valueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterValueWithVariable(GraphqlOperationParser.ValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#valueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitValueWithVariable(GraphqlOperationParser.ValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(GraphqlOperationParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(GraphqlOperationParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(GraphqlOperationParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(GraphqlOperationParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#stringValue}.
	 * @param ctx the parse tree
	 */
	void enterStringValue(GraphqlOperationParser.StringValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#stringValue}.
	 * @param ctx the parse tree
	 */
	void exitStringValue(GraphqlOperationParser.StringValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(GraphqlOperationParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(GraphqlOperationParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(GraphqlOperationParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(GraphqlOperationParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#listType}.
	 * @param ctx the parse tree
	 */
	void enterListType(GraphqlOperationParser.ListTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#listType}.
	 * @param ctx the parse tree
	 */
	void exitListType(GraphqlOperationParser.ListTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlOperationParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void enterNonNullType(GraphqlOperationParser.NonNullTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlOperationParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void exitNonNullType(GraphqlOperationParser.NonNullTypeContext ctx);
}