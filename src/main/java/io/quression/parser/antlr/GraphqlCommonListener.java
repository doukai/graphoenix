// Generated from GraphqlCommon.g4 by ANTLR 4.9
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link GraphqlCommonParser}.
 */
public interface GraphqlCommonListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#operationType}.
	 * @param ctx the parse tree
	 */
	void enterOperationType(GraphqlCommonParser.OperationTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#operationType}.
	 * @param ctx the parse tree
	 */
	void exitOperationType(GraphqlCommonParser.OperationTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#description}.
	 * @param ctx the parse tree
	 */
	void enterDescription(GraphqlCommonParser.DescriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#description}.
	 * @param ctx the parse tree
	 */
	void exitDescription(GraphqlCommonParser.DescriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#enumValue}.
	 * @param ctx the parse tree
	 */
	void enterEnumValue(GraphqlCommonParser.EnumValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#enumValue}.
	 * @param ctx the parse tree
	 */
	void exitEnumValue(GraphqlCommonParser.EnumValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#arrayValue}.
	 * @param ctx the parse tree
	 */
	void enterArrayValue(GraphqlCommonParser.ArrayValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#arrayValue}.
	 * @param ctx the parse tree
	 */
	void exitArrayValue(GraphqlCommonParser.ArrayValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterArrayValueWithVariable(GraphqlCommonParser.ArrayValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitArrayValueWithVariable(GraphqlCommonParser.ArrayValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#objectValue}.
	 * @param ctx the parse tree
	 */
	void enterObjectValue(GraphqlCommonParser.ObjectValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#objectValue}.
	 * @param ctx the parse tree
	 */
	void exitObjectValue(GraphqlCommonParser.ObjectValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterObjectValueWithVariable(GraphqlCommonParser.ObjectValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitObjectValueWithVariable(GraphqlCommonParser.ObjectValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#objectField}.
	 * @param ctx the parse tree
	 */
	void enterObjectField(GraphqlCommonParser.ObjectFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#objectField}.
	 * @param ctx the parse tree
	 */
	void exitObjectField(GraphqlCommonParser.ObjectFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterObjectFieldWithVariable(GraphqlCommonParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitObjectFieldWithVariable(GraphqlCommonParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#directives}.
	 * @param ctx the parse tree
	 */
	void enterDirectives(GraphqlCommonParser.DirectivesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#directives}.
	 * @param ctx the parse tree
	 */
	void exitDirectives(GraphqlCommonParser.DirectivesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterDirective(GraphqlCommonParser.DirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitDirective(GraphqlCommonParser.DirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(GraphqlCommonParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(GraphqlCommonParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(GraphqlCommonParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(GraphqlCommonParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#baseName}.
	 * @param ctx the parse tree
	 */
	void enterBaseName(GraphqlCommonParser.BaseNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#baseName}.
	 * @param ctx the parse tree
	 */
	void exitBaseName(GraphqlCommonParser.BaseNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void enterFragmentName(GraphqlCommonParser.FragmentNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void exitFragmentName(GraphqlCommonParser.FragmentNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#enumValueName}.
	 * @param ctx the parse tree
	 */
	void enterEnumValueName(GraphqlCommonParser.EnumValueNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#enumValueName}.
	 * @param ctx the parse tree
	 */
	void exitEnumValueName(GraphqlCommonParser.EnumValueNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(GraphqlCommonParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(GraphqlCommonParser.NameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(GraphqlCommonParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(GraphqlCommonParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#valueWithVariable}.
	 * @param ctx the parse tree
	 */
	void enterValueWithVariable(GraphqlCommonParser.ValueWithVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#valueWithVariable}.
	 * @param ctx the parse tree
	 */
	void exitValueWithVariable(GraphqlCommonParser.ValueWithVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(GraphqlCommonParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(GraphqlCommonParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(GraphqlCommonParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(GraphqlCommonParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#stringValue}.
	 * @param ctx the parse tree
	 */
	void enterStringValue(GraphqlCommonParser.StringValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#stringValue}.
	 * @param ctx the parse tree
	 */
	void exitStringValue(GraphqlCommonParser.StringValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(GraphqlCommonParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(GraphqlCommonParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(GraphqlCommonParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(GraphqlCommonParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#listType}.
	 * @param ctx the parse tree
	 */
	void enterListType(GraphqlCommonParser.ListTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#listType}.
	 * @param ctx the parse tree
	 */
	void exitListType(GraphqlCommonParser.ListTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphqlCommonParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void enterNonNullType(GraphqlCommonParser.NonNullTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphqlCommonParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void exitNonNullType(GraphqlCommonParser.NonNullTypeContext ctx);
}